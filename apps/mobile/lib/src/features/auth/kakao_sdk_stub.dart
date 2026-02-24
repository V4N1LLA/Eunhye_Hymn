import '../../core/network/api_exception.dart';

void initializeKakaoSdk(String nativeAppKey) {
  // Web/desktop에서는 Kakao SDK 초기화를 수행하지 않는다.
}

Future<String> fetchKakaoAccessToken() async {
  throw ApiException('카카오 SDK 직접 로그인은 모바일 앱에서만 지원됩니다.');
}
