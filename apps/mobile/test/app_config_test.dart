import 'package:flutter_test/flutter_test.dart';
import 'package:eunhye_hymn_mobile/src/core/config/app_config.dart';

void main() {
  test('API base URL always includes /api/v1', () {
    expect(AppConfig.apiBaseUrl.contains('/api/v1'), isTrue);
  });
}
