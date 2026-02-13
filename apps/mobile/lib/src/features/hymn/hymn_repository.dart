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
}

class HymnRepository {
  final ApiClient apiClient;

  HymnRepository({required this.apiClient});

  Future<List<HymnSummary>> listHymns() async {
    final raw = await apiClient.get('/hymns', includeAuth: false);
    if (raw is! List) {
      throw ApiException('찬양 목록 응답 형식이 올바르지 않습니다.');
    }

    return raw
        .whereType<Map<String, dynamic>>()
        .map((item) => HymnSummary(
              id: item['id'].toString(),
              title: item['title']?.toString() ?? '(제목 없음)',
              number: item['number']?.toString(),
              tags: item['tags']?.toString(),
            ))
        .toList();
  }

  Future<HymnDetail> getHymnDetail(String hymnId) async {
    final raw = await apiClient.get('/hymns/$hymnId');
    if (raw is! Map<String, dynamic>) {
      throw ApiException('찬양 상세 응답 형식이 올바르지 않습니다.');
    }

    final assetsRaw = raw['assets'];
    final assets = <HymnAsset>[];
    if (assetsRaw is List) {
      for (final item in assetsRaw.whereType<Map<String, dynamic>>()) {
        assets.add(HymnAsset(
          id: item['id'].toString(),
          type: item['type']?.toString() ?? '',
          part: item['part']?.toString(),
          url: item['url']?.toString() ?? '',
          checksum: item['checksum']?.toString(),
          version: item['version']?.toString(),
        ));
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

  Future<bool> toggleFavorite(String hymnId) async {
    final raw = await apiClient.post('/me/favorites/$hymnId');
    if (raw is! Map<String, dynamic>) {
      throw ApiException('즐겨찾기 응답 형식이 올바르지 않습니다.');
    }
    return raw['favorite'] == true;
  }

  Future<bool> getFavorite(String hymnId) async {
    final raw = await apiClient.get('/me/favorites/$hymnId');
    if (raw is! Map<String, dynamic>) {
      throw ApiException('즐겨찾기 조회 응답 형식이 올바르지 않습니다.');
    }
    return raw['favorite'] == true;
  }

  Future<String?> getNote(String hymnId) async {
    final raw = await apiClient.get('/me/hymns/$hymnId/note');
    if (raw is! Map<String, dynamic>) {
      throw ApiException('메모 응답 형식이 올바르지 않습니다.');
    }
    return raw['content']?.toString();
  }

  Future<String?> saveNote(String hymnId, String content) async {
    final trimmed = content.trim();
    if (trimmed.isEmpty) {
      throw ApiException('메모는 빈 값으로 저장할 수 없습니다.');
    }

    final raw = await apiClient.put(
      '/me/hymns/$hymnId/note',
      body: {'content': trimmed},
    );
    if (raw is! Map<String, dynamic>) {
      throw ApiException('메모 저장 응답 형식이 올바르지 않습니다.');
    }
    return raw['content']?.toString();
  }

  Future<List<HistoryItem>> getHistory() async {
    final raw = await apiClient.get('/me/history');
    if (raw is! List) {
      throw ApiException('히스토리 응답 형식이 올바르지 않습니다.');
    }

    return raw
        .whereType<Map<String, dynamic>>()
        .map((item) => HistoryItem(
              id: item['id'].toString(),
              title: item['title']?.toString() ?? '(제목 없음)',
              number: item['number']?.toString(),
              tags: item['tags']?.toString(),
              lastOpenedAt: item['lastOpenedAt']?.toString(),
            ))
        .toList();
  }
}
