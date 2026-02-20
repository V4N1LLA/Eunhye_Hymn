import 'dart:io';

import 'package:eunhye_hymn_mobile/src/features/hymn/hymn_repository.dart';
import 'package:eunhye_hymn_mobile/src/features/hymn/local_asset_cache.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:path/path.dart' as p;
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  late Directory tempDirectory;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    tempDirectory = await Directory.systemTemp.createTemp(
      'eunhye-asset-cache-test-',
    );
  });

  tearDown(() async {
    if (await tempDirectory.exists()) {
      await tempDirectory.delete(recursive: true);
    }
  });

  test('uses session-scoped cache paths per user', () async {
    var requestCount = 0;
    final cache = _createCache(
      client: _MockHttpClient((_) async {
        requestCount += 1;
        return http.Response.bytes(
          [1, 2, 3],
          200,
          headers: const {'content-type': 'image/png'},
        );
      }),
      tempDirectory: tempDirectory,
    );

    final asset = _imageAsset(id: 'asset-1', version: 'v1');
    final pathA = await cache.getOrDownload(asset, userId: 'user-A');
    final pathB = await cache.getOrDownload(asset, userId: 'user-B');

    expect(pathA, isNotNull);
    expect(pathB, isNotNull);
    expect(pathA, isNot(pathB));
    expect(requestCount, 2);
  });

  test('clearForUser removes only target user cache', () async {
    var requestCount = 0;
    final cache = _createCache(
      client: _MockHttpClient((_) async {
        requestCount += 1;
        return http.Response.bytes(
          [1, 2, 3],
          200,
          headers: const {'content-type': 'image/png'},
        );
      }),
      tempDirectory: tempDirectory,
    );

    final asset = _imageAsset(id: 'asset-clear', version: 'v1');
    final userAPath = await cache.getOrDownload(asset, userId: 'user-A');
    final userBPath = await cache.getOrDownload(asset, userId: 'user-B');
    expect(userAPath, isNotNull);
    expect(userBPath, isNotNull);
    expect(requestCount, 2);

    await cache.clearForUser(userId: 'user-A');

    final userAAfterClear = await cache.getOrDownload(asset, userId: 'user-A');
    final userBAfterClear = await cache.getOrDownload(asset, userId: 'user-B');

    expect(userAAfterClear, isNotNull);
    expect(userBAfterClear, equals(userBPath));
    expect(requestCount, 3);
  });

  test('reuses cached file for same user and version', () async {
    var requestCount = 0;
    final cache = _createCache(
      client: _MockHttpClient((_) async {
        requestCount += 1;
        return http.Response.bytes(
          [1, 2, 3],
          200,
          headers: const {'content-type': 'image/png'},
        );
      }),
      tempDirectory: tempDirectory,
    );

    final asset = _imageAsset(id: 'asset-2', version: 'v1');
    final firstPath = await cache.getOrDownload(asset, userId: 'member-1');
    final secondPath = await cache.getOrDownload(asset, userId: 'member-1');

    expect(firstPath, isNotNull);
    expect(secondPath, firstPath);
    expect(requestCount, 1);
  });

  test('rejects unexpected content-type for image assets', () async {
    final cache = _createCache(
      client: _MockHttpClient((_) async {
        return http.Response.bytes(
          [1, 2, 3],
          200,
          headers: const {'content-type': 'text/html'},
        );
      }),
      tempDirectory: tempDirectory,
    );

    final path = await cache.getOrDownload(
      _imageAsset(id: 'asset-3', version: 'v1'),
      userId: 'member-1',
    );

    expect(path, isNull);
  });

  test('rejects missing content-type for image assets', () async {
    final cache = _createCache(
      client: _MockHttpClient((_) async {
        return http.Response.bytes(
          [1, 2, 3],
          200,
        );
      }),
      tempDirectory: tempDirectory,
    );

    final path = await cache.getOrDownload(
      _imageAsset(id: 'asset-4', version: 'v1'),
      userId: 'member-1',
    );

    expect(path, isNull);
  });

  test('rejects payloads larger than max cache size while streaming', () async {
    final cache = _createCache(
      client: _MockStreamingHttpClient((_) async {
        return http.StreamedResponse(
          Stream<List<int>>.fromIterable([
            [0, 1, 2, 3],
            [4, 5, 6, 7],
          ]),
          200,
          headers: const {'content-type': 'image/png'},
        );
      }),
      tempDirectory: tempDirectory,
      maxAssetBytes: 6,
    );

    final path = await cache.getOrDownload(
      _imageAsset(id: 'asset-4', version: 'v1'),
      userId: 'member-1',
    );

    expect(path, isNull);
  });

  test('enforces total download timeout even when chunks keep arriving', () async {
    final cache = _createCache(
      client: _MockStreamingHttpClient((_) async {
        Stream<List<int>> stream() async* {
          for (var i = 0; i < 10; i++) {
            await Future<void>.delayed(const Duration(milliseconds: 15));
            yield [i];
          }
        }

        return http.StreamedResponse(
          stream(),
          200,
          headers: const {'content-type': 'image/png'},
        );
      }),
      tempDirectory: tempDirectory,
      downloadTimeout: const Duration(milliseconds: 40),
    );

    final path = await cache.getOrDownload(
      _imageAsset(id: 'asset-timeout', version: 'v1'),
      userId: 'member-1',
    );

    expect(path, isNull);
  });

  test('removes stale file when asset version changes', () async {
    var requestCount = 0;
    final cache = _createCache(
      client: _MockHttpClient((_) async {
        requestCount += 1;
        return http.Response.bytes(
          [requestCount, requestCount + 1],
          200,
          headers: const {'content-type': 'image/png'},
        );
      }),
      tempDirectory: tempDirectory,
    );

    final oldAsset = _imageAsset(id: 'asset-5', version: 'v1');
    final newAsset = _imageAsset(id: 'asset-5', version: 'v2');
    final oldPath = await cache.getOrDownload(oldAsset, userId: 'member-1');
    final newPath = await cache.getOrDownload(newAsset, userId: 'member-1');

    expect(oldPath, isNotNull);
    expect(newPath, isNotNull);
    expect(oldPath, isNot(newPath));
    expect(File(oldPath!).existsSync(), isFalse);
    expect(File(newPath!).existsSync(), isTrue);
  });

  test('sanitizes file names generated from asset id and version', () async {
    final cache = _createCache(
      client: _MockHttpClient((_) async {
        return http.Response.bytes(
          [1, 2, 3],
          200,
          headers: const {'content-type': 'image/png'},
        );
      }),
      tempDirectory: tempDirectory,
    );

    final path = await cache.getOrDownload(
      _imageAsset(id: '../bad:asset', version: '../../v1'),
      userId: 'member-1',
    );

    expect(path, isNotNull);
    final fileName = p.basename(path!);
    expect(fileName.contains('..'), isFalse);
    expect(fileName.contains('/'), isFalse);
    expect(fileName.contains('\\'), isFalse);
    expect(path.contains('../bad:asset'), isFalse);
    expect(path.contains('..\\bad:asset'), isFalse);
  });
}

LocalAssetCache _createCache({
  required http.Client client,
  required Directory tempDirectory,
  Duration downloadTimeout = const Duration(seconds: 20),
  int maxAssetBytes = 25 * 1024 * 1024,
}) {
  return LocalAssetCache(
    httpClient: client,
    prefsFactory: SharedPreferences.getInstance,
    documentsDirectoryProvider: () async => tempDirectory,
    now: () => DateTime.utc(2026, 2, 19),
    downloadTimeout: downloadTimeout,
    maxAssetBytes: maxAssetBytes,
  );
}

HymnAsset _imageAsset({required String id, required String version}) {
  return HymnAsset(
    id: id,
    type: 'PNG',
    part: 'ALL',
    url: 'https://cdn.example.com/hymns/score.png',
    checksum: null,
    version: version,
  );
}

class _MockHttpClient extends http.BaseClient {
  final Future<http.Response> Function(Uri uri) _handler;

  _MockHttpClient(this._handler);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final response = await _handler(request.url);
    return http.StreamedResponse(
      Stream<List<int>>.value(response.bodyBytes),
      response.statusCode,
      headers: response.headers,
      reasonPhrase: response.reasonPhrase,
      request: request,
    );
  }
}

class _MockStreamingHttpClient extends http.BaseClient {
  final Future<http.StreamedResponse> Function(Uri uri) _handler;

  _MockStreamingHttpClient(this._handler);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) {
    return _handler(request.url);
  }
}
