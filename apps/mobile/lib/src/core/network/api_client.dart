import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../storage/token_storage.dart';
import 'api_exception.dart';

class ApiClient {
  final String baseUrl;
  final TokenStorage tokenStorage;

  Future<bool>? _refreshInFlight;

  ApiClient({
    required this.baseUrl,
    required this.tokenStorage,
  });

  Future<dynamic> get(
    String path, {
    bool includeAuth = true,
  }) {
    return _request(
      method: 'GET',
      path: path,
      includeAuth: includeAuth,
    );
  }

  Future<dynamic> post(
    String path, {
    Object? body,
    bool includeAuth = true,
  }) {
    return _request(
      method: 'POST',
      path: path,
      body: body,
      includeAuth: includeAuth,
    );
  }

  Future<dynamic> put(
    String path, {
    Object? body,
    bool includeAuth = true,
  }) {
    return _request(
      method: 'PUT',
      path: path,
      body: body,
      includeAuth: includeAuth,
    );
  }

  Future<dynamic> patch(
    String path, {
    Object? body,
    bool includeAuth = true,
  }) {
    return _request(
      method: 'PATCH',
      path: path,
      body: body,
      includeAuth: includeAuth,
    );
  }

  Future<dynamic> delete(
    String path, {
    bool includeAuth = true,
  }) {
    return _request(
      method: 'DELETE',
      path: path,
      includeAuth: includeAuth,
    );
  }

  Future<dynamic> _request({
    required String method,
    required String path,
    Object? body,
    required bool includeAuth,
  }) async {
    final response = await _send(
      method: method,
      path: path,
      body: body,
      includeAuth: includeAuth,
    );

    if (response.statusCode == 401 &&
        includeAuth &&
        path != '/auth/refresh' &&
        method != 'OPTIONS') {
      final refreshed = await _refreshToken();
      if (refreshed) {
        final retried = await _send(
          method: method,
          path: path,
          body: body,
          includeAuth: includeAuth,
        );
        return _decodeEnvelope(retried);
      }
    }

    return _decodeEnvelope(response);
  }

  Future<http.Response> _send({
    required String method,
    required String path,
    Object? body,
    required bool includeAuth,
  }) async {
    final uri = Uri.parse('$baseUrl$path');
    final headers = <String, String>{};

    if (body != null) {
      headers['Content-Type'] = 'application/json';
    }

    if (includeAuth) {
      final accessToken = await tokenStorage.getAccessToken();
      if (accessToken != null && accessToken.isNotEmpty) {
        headers['Authorization'] = 'Bearer $accessToken';
      }
    }

    switch (method) {
      case 'GET':
        return http.get(uri, headers: headers);
      case 'POST':
        return http.post(uri, headers: headers, body: _encodeBody(body));
      case 'PUT':
        return http.put(uri, headers: headers, body: _encodeBody(body));
      case 'PATCH':
        return http.patch(uri, headers: headers, body: _encodeBody(body));
      case 'DELETE':
        return http.delete(uri, headers: headers);
      default:
        throw ApiException('지원하지 않는 HTTP 메서드입니다: $method');
    }
  }

  String? _encodeBody(Object? body) {
    if (body == null) {
      return null;
    }
    return jsonEncode(body);
  }

  Future<bool> _refreshToken() async {
    final inFlight = _refreshInFlight;
    if (inFlight != null) {
      return inFlight;
    }

    final task = _performRefresh();
    _refreshInFlight = task;
    try {
      return await task;
    } finally {
      _refreshInFlight = null;
    }
  }

  Future<bool> _performRefresh() async {
    final refreshToken = await tokenStorage.getRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      return false;
    }

    final uri = Uri.parse('$baseUrl/auth/refresh');
    final response = await http.post(
      uri,
      headers: const {'Content-Type': 'application/json'},
      body: jsonEncode({'refreshToken': refreshToken}),
    );

    final envelope = _decodeJsonObject(response.body);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      return false;
    }
    if (envelope == null || envelope['success'] != true) {
      return false;
    }

    final data = envelope['data'];
    if (data is! Map<String, dynamic>) {
      return false;
    }

    final accessToken = data['accessToken']?.toString();
    final rotatedRefreshToken = data['refreshToken']?.toString();
    if (accessToken == null ||
        accessToken.isEmpty ||
        rotatedRefreshToken == null ||
        rotatedRefreshToken.isEmpty) {
      return false;
    }

    await tokenStorage.saveTokens(
      accessToken: accessToken,
      refreshToken: rotatedRefreshToken,
    );
    return true;
  }

  dynamic _decodeEnvelope(http.Response response) {
    final hasBody = response.body.trim().isNotEmpty;
    final jsonMap = hasBody ? _decodeJsonObject(response.body) : null;

    if (response.statusCode == 401) {
      final message = _errorMessage(jsonMap, fallback: '인증이 만료되었습니다.');
      throw ApiException(
        message,
        statusCode: response.statusCode,
        code: _errorCode(jsonMap),
        details: _errorDetails(jsonMap),
      );
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = _errorMessage(jsonMap, fallback: '요청 처리 중 오류가 발생했습니다.');
      throw ApiException(
        message,
        statusCode: response.statusCode,
        code: _errorCode(jsonMap),
        details: _errorDetails(jsonMap),
      );
    }

    if (jsonMap == null) {
      return null;
    }

    if (jsonMap.containsKey('success')) {
      if (jsonMap['success'] != true) {
        final message = _errorMessage(jsonMap, fallback: '요청 처리 중 오류가 발생했습니다.');
        throw ApiException(
          message,
          statusCode: response.statusCode,
          code: _errorCode(jsonMap),
          details: _errorDetails(jsonMap),
        );
      }
      return jsonMap['data'];
    }

    return jsonMap;
  }

  Map<String, dynamic>? _decodeJsonObject(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) {
        return decoded;
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  String _errorMessage(
    Map<String, dynamic>? envelope, {
    required String fallback,
  }) {
    final error = envelope?['error'];
    if (error is Map<String, dynamic>) {
      final message = error['message']?.toString();
      if (message != null && message.isNotEmpty) {
        return message;
      }
    }
    return fallback;
  }

  String? _errorCode(Map<String, dynamic>? envelope) {
    final error = envelope?['error'];
    if (error is Map<String, dynamic>) {
      final code = error['code']?.toString();
      if (code != null && code.isNotEmpty) {
        return code;
      }
    }
    return null;
  }

  Object? _errorDetails(Map<String, dynamic>? envelope) {
    final error = envelope?['error'];
    if (error is Map<String, dynamic>) {
      return error['details'];
    }
    return null;
  }
}
