import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'hymn_repository.dart';

class LocalAssetCache {
  static const _indexKey = 'mobile.cache.assets.files.v1';
  static const _cacheDirName = 'hymn-assets';

  Future<String?> getOrDownload(HymnAsset asset) async {
    final cachedPath = await _resolveExistingPath(asset);
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
      final response = await http.get(uri).timeout(const Duration(seconds: 20));
      if (response.statusCode < 200 || response.statusCode >= 300) {
        return null;
      }

      final directory = await _ensureCacheDirectory();
      final fileName = _buildFileName(asset);
      final file = File(
        '${directory.path}${Platform.pathSeparator}$fileName',
      );
      await file.writeAsBytes(response.bodyBytes, flush: true);

      final index = await _readIndex();
      index[asset.id] = {
        'path': file.path,
        'version': asset.version,
        'updatedAt': DateTime.now().toIso8601String(),
      };
      await _writeIndex(index);

      return file.path;
    } catch (_) {
      return null;
    }
  }

  Future<String?> _resolveExistingPath(HymnAsset asset) async {
    final index = await _readIndex();
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
      await _removeEntry(index, asset.id, storedPath);
      return null;
    }

    final file = File(storedPath);
    if (!await file.exists()) {
      await _removeEntry(index, asset.id, storedPath);
      return null;
    }

    return storedPath;
  }

  Future<void> _removeEntry(
    Map<String, dynamic> index,
    String assetId,
    String filePath,
  ) async {
    try {
      final file = File(filePath);
      if (await file.exists()) {
        await file.delete();
      }
    } catch (_) {
      // Ignore cleanup failures; index is the source of truth.
    }
    index.remove(assetId);
    await _writeIndex(index);
  }

  String _buildFileName(HymnAsset asset) {
    final version = _sanitize(asset.version ?? 'v0');
    return '${asset.id}_$version${_extensionFor(asset)}';
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

  String _sanitize(String raw) {
    final cleaned = raw.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '_');
    return cleaned.isEmpty ? 'v0' : cleaned;
  }

  Future<Directory> _ensureCacheDirectory() async {
    final baseDir = await getApplicationDocumentsDirectory();
    final cacheDir = Directory(
      '${baseDir.path}${Platform.pathSeparator}$_cacheDirName',
    );
    if (!await cacheDir.exists()) {
      await cacheDir.create(recursive: true);
    }
    return cacheDir;
  }

  Future<Map<String, dynamic>> _readIndex() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_indexKey);
    if (raw == null || raw.isEmpty) {
      return <String, dynamic>{};
    }

    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) {
        return decoded;
      }
    } catch (_) {
      // Reset to empty if index is malformed.
    }
    return <String, dynamic>{};
  }

  Future<void> _writeIndex(Map<String, dynamic> index) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_indexKey, jsonEncode(index));
  }
}
