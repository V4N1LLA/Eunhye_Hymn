import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'hymn_repository.dart';

typedef CacheNowProvider = DateTime Function();
typedef CachePrefsFactory = Future<SharedPreferences> Function();
typedef CacheDirectoryProvider = Future<Directory> Function();

class LocalAssetCache {
  static const _indexKeyBase = 'mobile.cache.assets.files.v2';
  static const _cacheDirName = 'hymn-assets';
  static const _defaultDownloadTimeout = Duration(seconds: 20);
  static const _defaultMaxAssetBytes = 25 * 1024 * 1024;

  final http.Client _httpClient;
  final bool _ownsHttpClient;
  final CachePrefsFactory _prefsFactory;
  final CacheDirectoryProvider _documentsDirectoryProvider;
  final CacheNowProvider _now;
  final Duration _downloadTimeout;
  final int _maxAssetBytes;

  LocalAssetCache({
    http.Client? httpClient,
    CachePrefsFactory? prefsFactory,
    CacheDirectoryProvider? documentsDirectoryProvider,
    CacheNowProvider? now,
    Duration downloadTimeout = _defaultDownloadTimeout,
    int maxAssetBytes = _defaultMaxAssetBytes,
  })  : _httpClient = httpClient ?? http.Client(),
        _ownsHttpClient = httpClient == null,
        _prefsFactory = prefsFactory ?? SharedPreferences.getInstance,
        _documentsDirectoryProvider =
            documentsDirectoryProvider ?? getApplicationDocumentsDirectory,
        _now = now ?? DateTime.now,
        _downloadTimeout = downloadTimeout,
        _maxAssetBytes = maxAssetBytes;

  void dispose() {
    if (_ownsHttpClient) {
      _httpClient.close();
    }
  }

  Future<String?> getOrDownload(
    HymnAsset asset, {
    required String? userId,
  }) async {
    final normalizedUserId = _normalizeUserId(userId);
    if (normalizedUserId == null) {
      return null;
    }

    final cachedPath = await _resolveExistingPath(
      asset,
      normalizedUserId: normalizedUserId,
    );
    if (cachedPath != null) {
      return cachedPath;
    }

    if (!asset.url.startsWith('http')) {
      return null;
    }

    final uri = Uri.tryParse(asset.url);
    if (uri == null) {
      return null;
    }

    try {
      final request = http.Request('GET', uri);
      final response =
          await _httpClient.send(request).timeout(_downloadTimeout);
      if (response.statusCode < 200 || response.statusCode >= 300) {
        await _cancelStreamedResponse(response);
        return null;
      }

      if (!_supportsContentType(asset, response.headers['content-type'])) {
        await _cancelStreamedResponse(response);
        return null;
      }

      final announcedLength = response.contentLength;
      if (announcedLength != null && announcedLength > _maxAssetBytes) {
        await _cancelStreamedResponse(response);
        return null;
      }

      final bytes = await _readResponseBytesWithinLimit(response);
      if (bytes == null) {
        return null;
      }

      final directory = await _ensureCacheDirectory(normalizedUserId);
      final file = File(
        '${directory.path}${Platform.pathSeparator}${_buildFileName(asset)}',
      );
      await file.writeAsBytes(bytes, flush: true);

      final index = await _readIndex(normalizedUserId);
      index[asset.id] = {
        'path': file.path,
        'version': asset.version,
        'updatedAt': _now().toUtc().toIso8601String(),
      };
      await _writeIndex(index, normalizedUserId);
      return file.path;
    } catch (_) {
      return null;
    }
  }

  Future<void> clearForUser({required String userId}) async {
    final normalizedUserId = _normalizeUserId(userId);
    if (normalizedUserId == null) {
      return;
    }

    final index = await _readIndex(normalizedUserId);
    for (final entry in index.values) {
      if (entry is! Map<String, dynamic>) {
        continue;
      }
      final filePath = entry['path']?.toString();
      if (filePath == null || filePath.isEmpty) {
        continue;
      }
      try {
        final file = File(filePath);
        if (await file.exists()) {
          await file.delete();
        }
      } catch (_) {
        // Continue cleanup on a best-effort basis.
      }
    }

    final prefs = await _prefsFactory();
    await prefs.remove(_indexKeyForUser(normalizedUserId));

    try {
      final directory = await _cacheDirectoryForUser(normalizedUserId);
      if (await directory.exists()) {
        await directory.delete(recursive: true);
      }
    } catch (_) {
      // Continue cleanup on a best-effort basis.
    }
  }

  Future<Uint8List?> _readResponseBytesWithinLimit(
    http.StreamedResponse response,
  ) async {
    final bytes = BytesBuilder(copy: false);
    final completer = Completer<Uint8List?>();
    var totalBytes = 0;
    Timer? timer;

    late final StreamSubscription<List<int>> subscription;
    subscription = response.stream.listen(
      (chunk) {
        totalBytes += chunk.length;
        if (totalBytes > _maxAssetBytes) {
          timer?.cancel();
          subscription.cancel();
          if (!completer.isCompleted) {
            completer.complete(null);
          }
          return;
        }
        bytes.add(chunk);
      },
      onError: (_) {
        timer?.cancel();
        if (!completer.isCompleted) {
          completer.complete(null);
        }
      },
      onDone: () {
        timer?.cancel();
        if (!completer.isCompleted) {
          completer.complete(bytes.takeBytes());
        }
      },
      cancelOnError: true,
    );

    timer = Timer(_downloadTimeout, () {
      subscription.cancel();
      if (!completer.isCompleted) {
        completer.complete(null);
      }
    });

    try {
      return await completer.future;
    } finally {
      timer.cancel();
    }
  }

  Future<void> _cancelStreamedResponse(http.StreamedResponse response) async {
    try {
      await response.stream.listen((_) {}).cancel();
    } catch (_) {
      // Best effort cleanup only.
    }
  }

  Future<String?> _resolveExistingPath(
    HymnAsset asset, {
    required String normalizedUserId,
  }) async {
    final index = await _readIndex(normalizedUserId);
    final dynamic entry = index[asset.id];
    if (entry is! Map<String, dynamic>) {
      return null;
    }

    final storedPath = entry['path']?.toString();
    if (storedPath == null || storedPath.isEmpty) {
      return null;
    }

    final incomingVersion = (asset.version ?? '').trim();
    final storedVersion = entry['version']?.toString() ?? '';
    if (incomingVersion.isNotEmpty && incomingVersion != storedVersion) {
      await _removeEntry(
        index,
        assetId: asset.id,
        filePath: storedPath,
        normalizedUserId: normalizedUserId,
      );
      return null;
    }

    final file = File(storedPath);
    if (!await file.exists()) {
      await _removeEntry(
        index,
        assetId: asset.id,
        filePath: storedPath,
        normalizedUserId: normalizedUserId,
      );
      return null;
    }

    return storedPath;
  }

  Future<void> _removeEntry(
    Map<String, dynamic> index, {
    required String assetId,
    required String filePath,
    required String normalizedUserId,
  }) async {
    try {
      final file = File(filePath);
      if (await file.exists()) {
        await file.delete();
      }
    } catch (_) {
      // Best effort cleanup only.
    }
    index.remove(assetId);
    await _writeIndex(index, normalizedUserId);
  }

  String _buildFileName(HymnAsset asset) {
    final safeAssetId = _sanitize(asset.id, fallback: 'asset');
    final safeVersion = _sanitize(asset.version ?? 'v0', fallback: 'v0');
    return '$safeAssetId.$safeVersion${_extensionFor(asset)}';
  }

  String _extensionFor(HymnAsset asset) {
    if (asset.isImage) {
      return '.png';
    }
    if (asset.isMidi) {
      return '.mid';
    }

    final uri = Uri.tryParse(asset.url);
    if (uri == null) {
      return '.bin';
    }

    final path = uri.path;
    final dotIndex = path.lastIndexOf('.');
    if (dotIndex <= 0 || dotIndex >= path.length - 1) {
      return '.bin';
    }

    final extension = path.substring(dotIndex).toLowerCase();
    if (extension.length > 10) {
      return '.bin';
    }
    return extension;
  }

  bool _supportsContentType(HymnAsset asset, String? rawContentType) {
    if (rawContentType == null || rawContentType.isEmpty) {
      return false;
    }

    final contentType = rawContentType.split(';').first.trim().toLowerCase();

    if (asset.isImage) {
      return contentType.startsWith('image/') ||
          contentType == 'application/octet-stream';
    }

    if (asset.isMidi) {
      const allowedMidiTypes = {
        'audio/midi',
        'audio/mid',
        'audio/x-midi',
        'audio/sp-midi',
        'application/x-midi',
        'application/octet-stream',
      };
      return allowedMidiTypes.contains(contentType);
    }

    return contentType == 'application/octet-stream';
  }

  String? _normalizeUserId(String? userId) {
    if (userId == null) {
      return null;
    }
    final trimmed = userId.trim();
    if (trimmed.isEmpty) {
      return null;
    }
    return _sanitize(trimmed, fallback: 'anonymous');
  }

  String _sanitize(String raw, {required String fallback}) {
    final cleaned = raw.replaceAll(RegExp(r'[^A-Za-z0-9_-]'), '_');
    if (cleaned.isEmpty) {
      return fallback;
    }
    return cleaned;
  }

  Future<Directory> _ensureCacheDirectory(String normalizedUserId) async {
    final cacheDir = await _cacheDirectoryForUser(normalizedUserId);
    if (!await cacheDir.exists()) {
      await cacheDir.create(recursive: true);
    }
    return cacheDir;
  }

  Future<Directory> _cacheDirectoryForUser(String normalizedUserId) async {
    final baseDir = await _documentsDirectoryProvider();
    return Directory(
      '${baseDir.path}${Platform.pathSeparator}$_cacheDirName'
      '${Platform.pathSeparator}$normalizedUserId',
    );
  }

  String _indexKeyForUser(String normalizedUserId) {
    return '$_indexKeyBase.$normalizedUserId';
  }

  Future<Map<String, dynamic>> _readIndex(String normalizedUserId) async {
    final prefs = await _prefsFactory();
    final raw = prefs.getString(_indexKeyForUser(normalizedUserId));
    if (raw == null || raw.isEmpty) {
      return <String, dynamic>{};
    }

    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) {
        return decoded;
      }
    } catch (_) {
      // Reset to empty when index is malformed.
    }
    return <String, dynamic>{};
  }

  Future<void> _writeIndex(
    Map<String, dynamic> index,
    String normalizedUserId,
  ) async {
    final prefs = await _prefsFactory();
    await prefs.setString(
      _indexKeyForUser(normalizedUserId),
      jsonEncode(index),
    );
  }
}
