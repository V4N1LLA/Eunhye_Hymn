import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

import '../../core/network/api_exception.dart';

bool _kakaoInitialized = false;

void initializeKakaoSdk(String nativeAppKey) {
  if (nativeAppKey.isEmpty) {
    _kakaoInitialized = false;
    return;
  }
  KakaoSdk.init(nativeAppKey: nativeAppKey);
  _kakaoInitialized = true;
}

Future<String> fetchKakaoAccessToken() async {
  if (!_kakaoInitialized) {
    throw ApiException(
      'KAKAO_NATIVE_APP_KEY is empty. '
      'Set --dart-define=KAKAO_NATIVE_APP_KEY=<kakao_native_app_key>.',
    );
  }

  try {
    final isInstalled = await isKakaoTalkInstalled();
    OAuthToken token;
    if (isInstalled) {
      try {
        token = await UserApi.instance.loginWithKakaoTalk();
      } catch (_) {
        token = await UserApi.instance.loginWithKakaoAccount();
      }
    } else {
      token = await UserApi.instance.loginWithKakaoAccount();
    }

    if (token.accessToken.isEmpty) {
      throw ApiException('Kakao access token is missing.');
    }
    return token.accessToken;
  } on ApiException {
    rethrow;
  } catch (e) {
    throw ApiException('Kakao SDK sign-in failed: $e');
  }
}
