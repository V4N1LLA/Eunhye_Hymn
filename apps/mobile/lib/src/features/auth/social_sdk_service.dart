import 'package:flutter/foundation.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../../core/config/app_config.dart';
import '../../core/network/api_exception.dart';
import 'auth_repository.dart';
import 'kakao_sdk_stub.dart' if (dart.library.io) 'kakao_sdk_mobile.dart';

class SocialSdkService {
  final GoogleSignIn _googleSignIn;

  SocialSdkService({GoogleSignIn? googleSignIn})
      : _googleSignIn = googleSignIn ??
            GoogleSignIn(
              scopes: const ['email', 'profile'],
              clientId: kIsWeb && AppConfig.googleClientId.isNotEmpty
                  ? AppConfig.googleClientId
                  : null,
              serverClientId: AppConfig.googleServerClientId.isNotEmpty
                  ? AppConfig.googleServerClientId
                  : null,
            );

  void initialize() {
    initializeKakaoSdk(AppConfig.kakaoNativeAppKey);
  }

  Future<String> fetchToken(SocialProvider provider) {
    switch (provider) {
      case SocialProvider.google:
        return _fetchGoogleIdToken();
      case SocialProvider.kakao:
        return fetchKakaoAccessToken();
    }
  }

  Future<String> _fetchGoogleIdToken() async {
    try {
      GoogleSignInAccount? account = await _googleSignIn.signInSilently();
      account ??= await _googleSignIn.signIn();
      if (account == null) {
        throw ApiException('Google 로그인이 취소되었습니다.');
      }

      final auth = await account.authentication;
      final idToken = auth.idToken;
      if (idToken == null || idToken.isEmpty) {
        throw ApiException('Google ID Token을 가져오지 못했습니다. 클라이언트 설정을 확인하세요.');
      }
      return idToken;
    } on ApiException {
      rethrow;
    } catch (_) {
      throw ApiException('Google SDK 로그인에 실패했습니다.');
    }
  }

  Future<void> signOutGoogle() async {
    try {
      await _googleSignIn.signOut();
    } catch (_) {
      // 서버 로그아웃이 기준이며 SDK sign-out 실패는 무시한다.
    }
  }
}
