class AppConfig {
  static const _defaultApiBaseUrl = 'http://10.0.2.2:8080/api/v1';

  static final apiBaseUrl = normalizeApiBaseUrl(
    const String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: _defaultApiBaseUrl,
    ),
  );

  static const kakaoNativeAppKey = String.fromEnvironment(
    'KAKAO_NATIVE_APP_KEY',
    defaultValue: '',
  );

  static String normalizeApiBaseUrl(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) {
      return _defaultApiBaseUrl;
    }

    final uri = Uri.tryParse(trimmed);
    if (uri == null || uri.scheme.isEmpty || uri.host.isEmpty) {
      return trimmed;
    }

    final pathSegments =
        uri.pathSegments.where((segment) => segment.isNotEmpty);
    final normalized = pathSegments.toList(growable: true);

    final hasApiV1Suffix = normalized.length >= 2 &&
        normalized[normalized.length - 2] == 'api' &&
        normalized[normalized.length - 1] == 'v1';
    if (!hasApiV1Suffix) {
      normalized.addAll(['api', 'v1']);
    }

    return uri.replace(pathSegments: normalized).toString();
  }
}
