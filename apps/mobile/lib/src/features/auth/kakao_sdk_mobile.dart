import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

import '../../core/network/api_exception.dart';

void initializeKakaoSdk(String nativeAppKey) {
  if (nativeAppKey.isEmpty) {
    return;
  }
  KakaoSdk.init(nativeAppKey: nativeAppKey);
}

Future<String> fetchKakaoAccessToken() async {
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
      throw ApiException('카카오 Access Token을 가져오지 못했습니다.');
    }
    return token.accessToken;
  } on ApiException {
    rethrow;
  } catch (_) {
    throw ApiException('카카오 SDK 로그인에 실패했습니다. 카카오 설정을 확인하세요.');
  }
}
