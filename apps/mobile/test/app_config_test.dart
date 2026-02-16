import 'package:flutter_test/flutter_test.dart';
import 'package:eunhye_hymn_mobile/src/core/config/app_config.dart';

void main() {
  test('API base URL default always includes /api/v1', () {
    expect(AppConfig.apiBaseUrl.endsWith('/api/v1'), isTrue);
  });

  test('normalizes plain host to include /api/v1', () {
    expect(
      AppConfig.normalizeApiBaseUrl('http://13.209.200.12'),
      'http://13.209.200.12/api/v1',
    );
  });

  test('keeps explicit /api/v1 path', () {
    expect(
      AppConfig.normalizeApiBaseUrl('http://13.209.200.12/api/v1'),
      'http://13.209.200.12/api/v1',
    );
  });
}
