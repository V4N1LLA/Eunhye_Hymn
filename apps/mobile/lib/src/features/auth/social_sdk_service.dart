import '../../core/config/app_config.dart';
import 'kakao_sdk_stub.dart' if (dart.library.io) 'kakao_sdk_mobile.dart';

class SocialSdkService {
  void initialize() {
    initializeKakaoSdk(AppConfig.kakaoNativeAppKey);
  }

  Future<String> fetchToken() {
    return fetchKakaoAccessToken();
  }
}
