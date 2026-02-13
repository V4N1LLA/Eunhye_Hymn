import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../../core/storage/token_storage.dart';

enum SocialProvider { google, kakao }

enum UserRole { user, admin }

class SessionProfile {
  final String userId;
  final UserRole role;

  const SessionProfile({
    required this.userId,
    required this.role,
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
        throw ApiException('프로필 응답 형식이 올바르지 않습니다.');
      }
      return _toSessionProfile(raw);
    } on ApiException catch (e) {
      if (_isUnauthorized(e)) {
        return null;
      }
      rethrow;
    }
  }

  Future<SessionProfile> loginWithSocial({
    required SocialProvider provider,
    required String token,
    String? inviteCode,
  }) async {
    final raw = await apiClient.post(
      '/auth/social',
      includeAuth: false,
      body: {
        'provider': provider == SocialProvider.google ? 'GOOGLE' : 'KAKAO',
        'token': token,
        if (inviteCode != null && inviteCode.isNotEmpty) 'inviteCode': inviteCode,
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
      throw ApiException('로그인 후 프로필 조회에 실패했습니다.');
    }
    return profile;
  }

  Future<SessionProfile> loginWithDev({
    required String displayName,
    required String userId,
    required UserRole role,
  }) async {
    final raw = await apiClient.post(
      '/auth/dev/login',
      includeAuth: false,
      body: {
        'userId': userId,
        'role': role == UserRole.admin ? 'ADMIN' : 'USER',
        'displayName': displayName,
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
      throw ApiException('Dev 로그인 후 프로필 조회에 실패했습니다.');
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
        // 토큰 만료/폐기 상태에서도 로컬 세션 정리는 계속 진행한다.
      }
    }
    await tokenStorage.clear();
  }

  Future<void> clearSession() {
    return tokenStorage.clear();
  }

  SessionProfile _toSessionProfile(Map<String, dynamic> raw) {
    final userId = raw['userId']?.toString();
    final roleText = raw['role']?.toString().toUpperCase();
    if (userId == null || userId.isEmpty || roleText == null || roleText.isEmpty) {
      throw ApiException('프로필 응답에 필수 값이 없습니다.');
    }

    final role = roleText == 'ADMIN' ? UserRole.admin : UserRole.user;
    return SessionProfile(userId: userId, role: role);
  }

  bool _isUnauthorized(ApiException exception) {
    return exception.statusCode == 401 || exception.code == 'unauthorized';
  }

  (String, String) _extractTokens(Object? raw) {
    if (raw is! Map<String, dynamic>) {
      throw ApiException('토큰 응답 형식이 올바르지 않습니다.');
    }

    final accessToken = raw['accessToken']?.toString();
    final refreshToken = raw['refreshToken']?.toString();
    if (accessToken == null ||
        accessToken.isEmpty ||
        refreshToken == null ||
        refreshToken.isEmpty) {
      throw ApiException('토큰 응답에 필수 값이 없습니다.');
    }

    return (accessToken, refreshToken);
  }
}
