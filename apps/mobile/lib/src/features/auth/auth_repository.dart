import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../../core/storage/token_storage.dart';

enum UserRole { user, admin }

enum UserGender {
  male,
  female,
  unknown,
}

class SessionProfile {
  final String userId;
  final UserRole role;
  final String? displayName;
  final String? churchName;
  final String? name;
  final String? group;
  final UserGender gender;
  final bool profileCompleted;
  final bool inviteVerified;
  final bool phoneVerified;

  const SessionProfile({
    required this.userId,
    required this.role,
    required this.displayName,
    required this.churchName,
    required this.name,
    required this.group,
    required this.gender,
    required this.profileCompleted,
    required this.inviteVerified,
    required this.phoneVerified,
  });

  bool get isProfileCompleted {
    if (profileCompleted) {
      return true;
    }
    return (churchName?.trim().isNotEmpty ?? false) &&
        (name?.trim().isNotEmpty ?? false) &&
        (group?.trim().isNotEmpty ?? false);
  }

  bool get verified => inviteVerified && phoneVerified;

  SessionProfile copyWith({
    bool? inviteVerified,
    bool? phoneVerified,
    UserGender? gender,
  }) {
    return SessionProfile(
      userId: userId,
      role: role,
      displayName: displayName,
      churchName: churchName,
      name: name,
      group: group,
      gender: gender ?? this.gender,
      profileCompleted: profileCompleted,
      inviteVerified: inviteVerified ?? this.inviteVerified,
      phoneVerified: phoneVerified ?? this.phoneVerified,
    );
  }
}

class SmsRequestResult {
  final String verificationId;
  final int expiresInSeconds;
  final int cooldownSeconds;

  const SmsRequestResult({
    required this.verificationId,
    required this.expiresInSeconds,
    required this.cooldownSeconds,
  });
}

class SmsVerifyResult {
  final bool verified;
  final bool completed;

  const SmsVerifyResult({
    required this.verified,
    required this.completed,
  });
}

enum ProfileChangeRequestStatus {
  pending,
  approved,
  rejected,
}

class ProfileChangeRequestResult {
  final String id;
  final ProfileChangeRequestStatus status;
  final UserGender gender;
  final DateTime requestedAt;
  final DateTime? reviewedAt;
  final String? rejectReason;

  const ProfileChangeRequestResult({
    required this.id,
    required this.status,
    required this.gender,
    required this.requestedAt,
    required this.reviewedAt,
    required this.rejectReason,
  });
}

class AuthRepository {
  final ApiClient apiClient;
  final TokenStorage tokenStorage;

  AuthRepository({
    required this.apiClient,
    required this.tokenStorage,
  });

  Future<bool> hasSession() {
    return tokenStorage.hasTokens();
  }

  Future<SessionProfile?> fetchProfile() async {
    try {
      final raw = await apiClient.get('/me/profile');
      if (raw is! Map<String, dynamic>) {
        throw ApiException('Profile response is invalid.');
      }
      return _toSessionProfile(raw);
    } on ApiException catch (e) {
      if (_isUnauthorized(e)) {
        return null;
      }
      rethrow;
    }
  }

  Future<SessionProfile> loginWithKakao({
    required String token,
  }) {
    return _loginWithSocialToken(token: token, inviteCode: null);
  }

  Future<SessionProfile> loginWithSocial({
    required String token,
    String? inviteCode,
  }) {
    return _loginWithSocialToken(token: token, inviteCode: inviteCode);
  }

  Future<bool> verifyInviteCode(String inviteCode) async {
    final normalizedCode = inviteCode.trim().toUpperCase();
    if (normalizedCode.isEmpty) {
      throw ApiException('Invite code is required.');
    }

    final raw = await apiClient.post(
      '/auth/invite/validate',
      body: {'code': normalizedCode},
    );
    if (raw is! Map<String, dynamic>) {
      throw ApiException('Invite verification response is invalid.');
    }
    return _toBool(raw['valid']);
  }

  Future<SmsRequestResult> requestSmsCode(String phoneNumber) async {
    final raw = await apiClient.post(
      '/auth/sms/request',
      body: {'phoneNumber': phoneNumber},
    );
    if (raw is! Map<String, dynamic>) {
      throw ApiException('SMS request response is invalid.');
    }

    final verificationId = raw['verificationId']?.toString();
    if (verificationId == null || verificationId.isEmpty) {
      throw ApiException('SMS request response is missing verificationId.');
    }

    return SmsRequestResult(
      verificationId: verificationId,
      expiresInSeconds: _toInt(raw['expiresInSeconds'], fallback: 0),
      cooldownSeconds: _toInt(raw['cooldownSeconds'], fallback: 0),
    );
  }

  Future<SmsVerifyResult> verifySmsCode(
    String verificationId,
    String code,
  ) async {
    final raw = await apiClient.post(
      '/auth/sms/verify',
      body: {
        'verificationId': verificationId,
        'code': code,
      },
    );
    if (raw is! Map<String, dynamic>) {
      throw ApiException('SMS verification response is invalid.');
    }

    return SmsVerifyResult(
      verified: _toBool(raw['verified']),
      completed: _toBool(raw['completed']),
    );
  }

  Future<void> withdrawAccount() async {
    await apiClient.post('/auth/withdraw');
  }

  Future<SessionProfile> signupWithAccount({
    required String loginId,
    required String password,
    required String inviteCode,
  }) {
    return _loginWithPasswordEndpoint(
      '/auth/signup',
      body: {
        'loginId': loginId,
        'password': password,
        'inviteCode': inviteCode,
      },
    );
  }

  Future<SessionProfile> loginWithAccount({
    required String loginId,
    required String password,
  }) {
    return _loginWithPasswordEndpoint(
      '/auth/login',
      body: {
        'loginId': loginId,
        'password': password,
      },
    );
  }

  Future<SessionProfile> loginWithAdmin({
    required String loginId,
    required String password,
  }) async {
    final raw = await apiClient.post(
      '/auth/admin/login',
      includeAuth: false,
      body: {
        'loginId': loginId,
        'password': password,
      },
    );

    final tokens = _extractTokens(raw);
    await tokenStorage.saveTokens(
      accessToken: tokens.$1,
      refreshToken: tokens.$2,
    );

    final profile = await fetchProfile();
    if (profile == null) {
      await tokenStorage.clear();
      throw ApiException('Login response is not usable.');
    }
    return profile;
  }

  Future<void> logout() async {
    final refreshToken = await tokenStorage.getRefreshToken();
    if (refreshToken != null && refreshToken.isNotEmpty) {
      try {
        await apiClient.post(
          '/auth/logout',
          includeAuth: false,
          body: {'refreshToken': refreshToken},
        );
      } on ApiException {
        // Ignore logout failures and clear local tokens anyway.
      }
    }
    await tokenStorage.clear();
  }

  Future<void> clearSession() {
    return tokenStorage.clear();
  }

  Future<ProfileChangeRequestResult> requestProfileChange({
    required String churchName,
    required String name,
    required String group,
    required UserGender gender,
  }) async {
    final raw = await apiClient.post(
      '/me/profile-change-requests',
      body: {
        'churchName': churchName.trim(),
        'name': name.trim(),
        'group': group.trim(),
        'gender': _toGenderApiValue(gender),
      },
    );
    if (raw is! Map<String, dynamic>) {
      throw ApiException('Profile change request response is invalid.');
    }
    return _toProfileChangeRequestResult(raw);
  }

  Future<ProfileChangeRequestResult?> fetchLatestProfileChangeRequest() async {
    try {
      final raw = await apiClient.get('/me/profile-change-requests/latest');
      if (raw == null) {
        return null;
      }
      if (raw is! Map<String, dynamic>) {
        throw ApiException('Profile change request response is invalid.');
      }
      return _toProfileChangeRequestResult(raw);
    } on ApiException catch (e) {
      if (e.statusCode == 404 || e.code == 'profile_change_request_not_found') {
        return null;
      }
      rethrow;
    }
  }

  Future<SessionProfile> updateProfile({
    required String churchName,
    required String name,
    required String group,
    required UserGender gender,
  }) async {
    final raw = await apiClient.put(
      '/me/profile',
      body: {
        'churchName': churchName.trim(),
        'name': name.trim(),
        'group': group.trim(),
        'gender': _toGenderApiValue(gender),
      },
    );

    if (raw is! Map<String, dynamic>) {
      throw ApiException('Profile response is invalid.');
    }

    return _toSessionProfile(raw);
  }

  Future<SessionProfile> _loginWithSocialToken({
    required String token,
    String? inviteCode,
  }) async {
    final raw = await apiClient.post(
      '/auth/social',
      includeAuth: false,
      body: {
        'provider': 'KAKAO',
        'token': token,
        if (inviteCode != null && inviteCode.trim().isNotEmpty)
          'inviteCode': inviteCode.trim().toUpperCase(),
      },
    );

    final tokens = _extractTokens(raw);
    await tokenStorage.saveTokens(
      accessToken: tokens.$1,
      refreshToken: tokens.$2,
    );

    final profile = await fetchProfile();
    if (profile == null) {
      await tokenStorage.clear();
      throw ApiException('Login response is not usable.');
    }
    return profile;
  }

  Future<SessionProfile> _loginWithPasswordEndpoint(
    String path, {
    required Map<String, Object?> body,
  }) async {
    final raw = await apiClient.post(
      path,
      includeAuth: false,
      body: body,
    );

    final tokens = _extractTokens(raw);
    await tokenStorage.saveTokens(
      accessToken: tokens.$1,
      refreshToken: tokens.$2,
    );

    final profile = await fetchProfile();
    if (profile == null) {
      await tokenStorage.clear();
      throw ApiException('Login response is not usable.');
    }
    return profile;
  }

  SessionProfile _toSessionProfile(Map<String, dynamic> raw) {
    final userId = raw['userId']?.toString();
    final roleText = raw['role']?.toString().toUpperCase();
    if (userId == null ||
        userId.isEmpty ||
        roleText == null ||
        roleText.isEmpty) {
      throw ApiException('Profile response is invalid.');
    }

    final role = roleText == 'ADMIN' ? UserRole.admin : UserRole.user;
    var inviteVerified = _toBool(raw['inviteVerified']);
    var phoneVerified = _toBool(raw['phoneVerified']);
    if (_toBool(raw['verified'])) {
      inviteVerified = true;
      phoneVerified = true;
    }
    if (role == UserRole.admin) {
      inviteVerified = true;
      phoneVerified = true;
    }

    return SessionProfile(
      userId: userId,
      role: role,
      displayName: _toNullableText(raw['displayName']),
      churchName: _toNullableText(raw['churchName']),
      name: _toNullableText(raw['name']),
      group: _toNullableText(raw['group']),
      gender: _toUserGender(raw['gender']),
      profileCompleted: raw['profileCompleted'] == true,
      inviteVerified: inviteVerified,
      phoneVerified: phoneVerified,
    );
  }

  ProfileChangeRequestResult _toProfileChangeRequestResult(
    Map<String, dynamic> raw,
  ) {
    final id = raw['id']?.toString();
    final statusRaw = raw['status']?.toString();
    final requestedAtRaw = raw['requestedAt']?.toString();

    if (id == null ||
        id.isEmpty ||
        statusRaw == null ||
        statusRaw.isEmpty ||
        requestedAtRaw == null ||
        requestedAtRaw.isEmpty) {
      throw ApiException('Profile change request response is invalid.');
    }

    return ProfileChangeRequestResult(
      id: id,
      status: _toProfileChangeRequestStatus(statusRaw),
      gender: _toUserGender(raw['gender']),
      requestedAt: DateTime.parse(requestedAtRaw),
      reviewedAt: _toDateTime(raw['reviewedAt']),
      rejectReason: _toNullableText(raw['rejectReason']),
    );
  }

  bool _isUnauthorized(ApiException exception) {
    return exception.statusCode == 401 || exception.code == 'unauthorized';
  }

  (String, String) _extractTokens(Object? raw) {
    if (raw is! Map<String, dynamic>) {
      throw ApiException('API response is invalid.');
    }

    final accessToken = raw['accessToken']?.toString();
    final refreshToken = raw['refreshToken']?.toString();
    if (accessToken == null ||
        accessToken.isEmpty ||
        refreshToken == null ||
        refreshToken.isEmpty) {
      throw ApiException('API response has invalid token data.');
    }

    return (accessToken, refreshToken);
  }

  String? _toNullableText(Object? value) {
    final text = value?.toString();
    if (text == null || text.trim().isEmpty) {
      return null;
    }
    return text.trim();
  }

  DateTime? _toDateTime(Object? value) {
    final text = value?.toString();
    if (text == null || text.trim().isEmpty) {
      return null;
    }
    try {
      return DateTime.parse(text.trim());
    } catch (_) {
      return null;
    }
  }

  ProfileChangeRequestStatus _toProfileChangeRequestStatus(String raw) {
    return switch (raw.trim().toUpperCase()) {
      'APPROVED' => ProfileChangeRequestStatus.approved,
      'REJECTED' => ProfileChangeRequestStatus.rejected,
      _ => ProfileChangeRequestStatus.pending,
    };
  }

  UserGender _toUserGender(Object? raw) {
    final normalized = raw?.toString().trim().toUpperCase();
    return switch (normalized) {
      'MALE' => UserGender.male,
      'FEMALE' => UserGender.female,
      _ => UserGender.unknown,
    };
  }

  String _toGenderApiValue(UserGender gender) {
    return switch (gender) {
      UserGender.male => 'MALE',
      UserGender.female => 'FEMALE',
      UserGender.unknown => 'UNKNOWN',
    };
  }

  bool _toBool(Object? raw, {bool fallback = false}) {
    if (raw is bool) {
      return raw;
    }
    if (raw is String) {
      final normalized = raw.trim().toLowerCase();
      if (normalized == 'true') {
        return true;
      }
      if (normalized == 'false') {
        return false;
      }
    }
    if (raw is num) {
      return raw != 0;
    }
    return fallback;
  }

  int _toInt(Object? raw, {required int fallback}) {
    if (raw is int) {
      return raw;
    }
    if (raw is num) {
      return raw.toInt();
    }
    if (raw is String) {
      return int.tryParse(raw) ?? fallback;
    }
    return fallback;
  }
}
