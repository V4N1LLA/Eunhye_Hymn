import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';

class HymnSummary {
  final String id;
  final String title;
  final String? number;
  final String? tags;

  const HymnSummary({
    required this.id,
    required this.title,
    required this.number,
    required this.tags,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'number': number,
      'tags': tags,
    };
  }

  factory HymnSummary.fromJson(Map<String, dynamic> item) {
    return HymnSummary(
      id: item['id'].toString(),
      title: item['title']?.toString() ?? '(제목 없음)',
      number: item['number']?.toString(),
      tags: item['tags']?.toString(),
    );
  }
}

class HymnAsset {
  final String id;
  final String type;
  final String? part;
  final String url;
  final String? checksum;
  final String? version;

  const HymnAsset({
    required this.id,
    required this.type,
    required this.part,
    required this.url,
    required this.checksum,
    required this.version,
  });

  bool get isImage => type.toUpperCase() == 'PNG';
  bool get isMidi => type.toUpperCase() == 'MIDI';

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'type': type,
      'part': part,
      'url': url,
      'checksum': checksum,
      'version': version,
    };
  }

  factory HymnAsset.fromJson(Map<String, dynamic> item) {
    return HymnAsset(
      id: item['id'].toString(),
      type: item['type']?.toString() ?? '',
      part: item['part']?.toString(),
      url: item['url']?.toString() ?? '',
      checksum: item['checksum']?.toString(),
      version: item['version']?.toString(),
    );
  }
}

class HymnDetail {
  final String id;
  final String title;
  final String? number;
  final String? tags;
  final bool enabled;
  final String? lastOpenedAt;
  final List<HymnAsset> assets;

  const HymnDetail({
    required this.id,
    required this.title,
    required this.number,
    required this.tags,
    required this.enabled,
    required this.lastOpenedAt,
    required this.assets,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'number': number,
      'tags': tags,
      'enabled': enabled,
      'lastOpenedAt': lastOpenedAt,
      'assets': assets.map((asset) => asset.toJson()).toList(),
    };
  }

  factory HymnDetail.fromJson(Map<String, dynamic> raw) {
    final assetsRaw = raw['assets'];
    final assets = <HymnAsset>[];
    if (assetsRaw is List) {
      for (final item in assetsRaw.whereType<Map<String, dynamic>>()) {
        assets.add(HymnAsset.fromJson(item));
      }
    }

    return HymnDetail(
      id: raw['id'].toString(),
      title: raw['title']?.toString() ?? '(제목 없음)',
      number: raw['number']?.toString(),
      tags: raw['tags']?.toString(),
      enabled: raw['enabled'] == true,
      lastOpenedAt: raw['lastOpenedAt']?.toString(),
      assets: assets,
    );
  }
}

class HistoryItem {
  final String id;
  final String title;
  final String? number;
  final String? tags;
  final String? lastOpenedAt;

  const HistoryItem({
    required this.id,
    required this.title,
    required this.number,
    required this.tags,
    required this.lastOpenedAt,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'number': number,
      'tags': tags,
      'lastOpenedAt': lastOpenedAt,
    };
  }

  factory HistoryItem.fromJson(Map<String, dynamic> item) {
    return HistoryItem(
      id: item['id'].toString(),
      title: item['title']?.toString() ?? '(제목 없음)',
      number: item['number']?.toString(),
      tags: item['tags']?.toString(),
      lastOpenedAt: item['lastOpenedAt']?.toString(),
    );
  }
}

class HymnRepository {
  final ApiClient apiClient;
  String? _sessionUserId;

  HymnRepository({required this.apiClient});

  String? get sessionUserId => _sessionUserId;

  static const _hymnListCacheKey = 'mobile.cache.hymn.list';
  static const _historyCacheKey = 'mobile.cache.history';
  static const _pendingActionsKey = 'mobile.cache.pending.actions';

  void bindSessionUser(String? userId) {
    _sessionUserId = userId;
  }

  Future<List<HymnSummary>> listHymns() async {
    try {
      final raw = await apiClient.get('/hymns', includeAuth: false);
      if (raw is! List) {
        throw ApiException('찬양 목록 응답 형식이 올바르지 않습니다.');
      }

      final items = raw
          .whereType<Map<String, dynamic>>()
          .map(HymnSummary.fromJson)
          .toList();
      await _writeJsonList(
        _hymnListCacheKey,
        items.map((item) => item.toJson()).toList(),
      );
      return items;
    } catch (_) {
      final cached = await _readJsonList(_hymnListCacheKey);
      if (cached != null) {
        return cached
            .whereType<Map<String, dynamic>>()
            .map(HymnSummary.fromJson)
            .toList();
      }
      rethrow;
    }
  }

  Future<HymnDetail> getHymnDetail(String hymnId) async {
    try {
      final raw = await apiClient.get('/hymns/$hymnId');
      if (raw is! Map<String, dynamic>) {
        throw ApiException('찬양 상세 응답 형식이 올바르지 않습니다.');
      }

      final detail = HymnDetail.fromJson(raw);
      await _writeJsonObject(_detailCacheKey(hymnId), detail.toJson());
      return detail;
    } catch (_) {
      final cached = await _readJsonObject(_detailCacheKey(hymnId));
      if (cached != null) {
        return HymnDetail.fromJson(cached);
      }
      rethrow;
    }
  }

  Future<bool> toggleFavorite(String hymnId) async {
    try {
      final raw = await apiClient.post('/me/favorites/$hymnId');
      if (raw is! Map<String, dynamic>) {
        throw ApiException('즐겨찾기 응답 형식이 올바르지 않습니다.');
      }
      final favorite = raw['favorite'] == true;
      await _writeFavoriteCache(hymnId, favorite);
      return favorite;
    } catch (_) {
      final current = await _readFavoriteCache(hymnId) ?? false;
      final next = !current;
      await _writeFavoriteCache(hymnId, next);
      await _enqueuePendingAction(_PendingAction.toggleFavorite(hymnId));
      return next;
    }
  }

  Future<bool> getFavorite(String hymnId) async {
    try {
      final raw = await apiClient.get('/me/favorites/$hymnId');
      if (raw is! Map<String, dynamic>) {
        throw ApiException('즐겨찾기 조회 응답 형식이 올바르지 않습니다.');
      }
      final favorite = raw['favorite'] == true;
      await _writeFavoriteCache(hymnId, favorite);
      return favorite;
    } catch (_) {
      return await _readFavoriteCache(hymnId) ?? false;
    }
  }

  Future<String?> getNote(String hymnId) async {
    try {
      final raw = await apiClient.get('/me/hymns/$hymnId/note');
      if (raw is! Map<String, dynamic>) {
        throw ApiException('메모 응답 형식이 올바르지 않습니다.');
      }
      final content = raw['content']?.toString();
      await _writeNoteCache(hymnId, content);
      return content;
    } catch (_) {
      return _readNoteCache(hymnId);
    }
  }

  Future<String?> saveNote(String hymnId, String content) async {
    final trimmed = content.trim();
    if (trimmed.isEmpty) {
      throw ApiException('메모는 빈 값으로 저장할 수 없습니다.');
    }

    try {
      final raw = await apiClient.put(
        '/me/hymns/$hymnId/note',
        body: {'content': trimmed},
      );
      if (raw is! Map<String, dynamic>) {
        throw ApiException('메모 저장 응답 형식이 올바르지 않습니다.');
      }
      final saved = raw['content']?.toString() ?? trimmed;
      await _writeNoteCache(hymnId, saved);
      return saved;
    } catch (_) {
      await _writeNoteCache(hymnId, trimmed);
      await _enqueuePendingAction(_PendingAction.saveNote(hymnId, trimmed));
      return trimmed;
    }
  }

  Future<List<HistoryItem>> getHistory() async {
    try {
      final raw = await apiClient.get('/me/history');
      if (raw is! List) {
        throw ApiException('히스토리 응답 형식이 올바르지 않습니다.');
      }

      final items = raw
          .whereType<Map<String, dynamic>>()
          .map(HistoryItem.fromJson)
          .toList();
      await _writeJsonList(
        _userScopedKey(_historyCacheKey),
        items.map((item) => item.toJson()).toList(),
      );
      return items;
    } catch (_) {
      final cached = await _readJsonList(_userScopedKey(_historyCacheKey));
      if (cached != null) {
        return cached
            .whereType<Map<String, dynamic>>()
            .map(HistoryItem.fromJson)
            .toList();
      }
      rethrow;
    }
  }

  Future<void> syncPendingActions() async {
    final queued = await _readPendingActions();
    if (queued.isEmpty) {
      return;
    }

    final remain = <_PendingAction>[];
    for (final action in queued) {
      try {
        switch (action.type) {
          case _PendingActionType.toggleFavorite:
            final raw = await apiClient.post('/me/favorites/${action.hymnId}');
            if (raw is Map<String, dynamic>) {
              await _writeFavoriteCache(action.hymnId, raw['favorite'] == true);
            }
            break;
          case _PendingActionType.saveNote:
            await apiClient.put(
              '/me/hymns/${action.hymnId}/note',
              body: {'content': action.content},
            );
            if (action.content != null) {
              await _writeNoteCache(action.hymnId, action.content);
            }
            break;
        }
      } catch (_) {
        remain.add(action);
      }
    }

    await _writeJsonList(
      _userScopedKey(_pendingActionsKey),
      remain.map((item) => item.toJson()).toList(),
    );
  }

  String _detailCacheKey(String hymnId) => 'mobile.cache.hymn.detail.$hymnId';
  String _favoriteCacheKey(String hymnId) =>
      _userScopedKey('mobile.cache.hymn.favorite.$hymnId');
  String _noteCacheKey(String hymnId) =>
      _userScopedKey('mobile.cache.hymn.note.$hymnId');

  Future<void> _writeFavoriteCache(String hymnId, bool favorite) async {
    await _writeJsonObject(_favoriteCacheKey(hymnId), {'favorite': favorite});
  }

  Future<bool?> _readFavoriteCache(String hymnId) async {
    final raw = await _readJsonObject(_favoriteCacheKey(hymnId));
    if (raw == null) {
      return null;
    }
    return raw['favorite'] == true;
  }

  Future<void> _writeNoteCache(String hymnId, String? content) async {
    await _writeJsonObject(_noteCacheKey(hymnId), {'content': content});
  }

  Future<String?> _readNoteCache(String hymnId) async {
    final raw = await _readJsonObject(_noteCacheKey(hymnId));
    return raw?['content']?.toString();
  }

  Future<void> _enqueuePendingAction(_PendingAction action) async {
    final queued = await _readPendingActions();
    queued.add(action);
    await _writeJsonList(
      _userScopedKey(_pendingActionsKey),
      queued.map((item) => item.toJson()).toList(),
    );
  }

  Future<List<_PendingAction>> _readPendingActions() async {
    final raw = await _readJsonList(_userScopedKey(_pendingActionsKey));
    if (raw == null) {
      return const [];
    }

    return raw
        .whereType<Map<String, dynamic>>()
        .map((item) {
          try {
            return _PendingAction.fromJson(item);
          } catch (_) {
            return null;
          }
        })
        .whereType<_PendingAction>()
        .toList();
  }

  String _userScopedKey(String base) {
    final userId = _sessionUserId;
    if (userId == null || userId.isEmpty) {
      throw ApiException('로그인 세션이 없습니다.');
    }
    return '$base.$userId';
  }

  Future<void> _writeJsonObject(String key, Map<String, dynamic> value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(key, jsonEncode(value));
  }

  Future<Map<String, dynamic>?> _readJsonObject(String key) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(key);
    if (raw == null || raw.isEmpty) {
      return null;
    }

    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) {
        return decoded;
      }
    } catch (_) {
      return null;
    }
    return null;
  }

  Future<void> _writeJsonList(
      String key, List<Map<String, dynamic>> value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(key, jsonEncode(value));
  }

  Future<List<dynamic>?> _readJsonList(String key) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(key);
    if (raw == null || raw.isEmpty) {
      return null;
    }

    try {
      final decoded = jsonDecode(raw);
      if (decoded is List<dynamic>) {
        return decoded;
      }
    } catch (_) {
      return null;
    }
    return null;
  }
}

enum _PendingActionType { toggleFavorite, saveNote }

class _PendingAction {
  final _PendingActionType type;
  final String hymnId;
  final String? content;

  const _PendingAction({
    required this.type,
    required this.hymnId,
    this.content,
  });

  factory _PendingAction.toggleFavorite(String hymnId) {
    return _PendingAction(
      type: _PendingActionType.toggleFavorite,
      hymnId: hymnId,
    );
  }

  factory _PendingAction.saveNote(String hymnId, String content) {
    return _PendingAction(
      type: _PendingActionType.saveNote,
      hymnId: hymnId,
      content: content,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'type': type.name,
      'hymnId': hymnId,
      'content': content,
    };
  }

  factory _PendingAction.fromJson(Map<String, dynamic> raw) {
    final typeText = raw['type']?.toString();
    final hymnId = raw['hymnId']?.toString();
    if (typeText == null || hymnId == null || hymnId.isEmpty) {
      throw const FormatException('invalid pending action');
    }

    final type = _PendingActionType.values.firstWhere(
      (item) => item.name == typeText,
      orElse: () => _PendingActionType.toggleFavorite,
    );

    return _PendingAction(
      type: type,
      hymnId: hymnId,
      content: raw['content']?.toString(),
    );
  }
}
