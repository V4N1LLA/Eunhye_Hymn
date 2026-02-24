class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final String? code;
  final Object? details;

  ApiException(
    this.message, {
    this.statusCode,
    this.code,
    this.details,
  });

  @override
  String toString() {
    final status = statusCode != null ? ' [$statusCode]' : '';
    final errCode = code != null ? ' ($code)' : '';
    return '$message$status$errCode';
  }
}
