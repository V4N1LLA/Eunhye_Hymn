import 'dart:async';
import 'dart:io';

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import 'hymn_repository.dart';
import 'local_asset_cache.dart';

class HymnDetailPage extends StatefulWidget {
  final String hymnId;
  final HymnRepository hymnRepository;

  const HymnDetailPage({
    super.key,
    required this.hymnId,
    required this.hymnRepository,
  });

  @override
  State<HymnDetailPage> createState() => _HymnDetailPageState();
}

class _HymnDetailPageState extends State<HymnDetailPage> {
  static const _assetPrefetchBatchSize = 3;

  final _noteController = TextEditingController();
  final _localAssetCache = LocalAssetCache();
  final Map<String, String> _localAssetPaths = {};
  int _assetLoadSequence = 0;

  bool _loading = true;
  bool _favorite = false;
  bool _savingNote = false;
  bool _togglingFavorite = false;
  String? _error;
  HymnDetail? _detail;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _localAssetCache.dispose();
    _noteController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final detail = await widget.hymnRepository.getHymnDetail(widget.hymnId);
      final note = await widget.hymnRepository.getNote(widget.hymnId);
      final favorite = await widget.hymnRepository.getFavorite(widget.hymnId);

      if (!mounted) {
        return;
      }
      final sequence = ++_assetLoadSequence;
      setState(() {
        _detail = detail;
        _noteController.text = note ?? '';
        _favorite = favorite;
        _localAssetPaths.clear();
      });
      unawaited(_cacheAssets(detail.assets, sequence));
    } catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _error = e.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  Future<void> _cacheAssets(List<HymnAsset> assets, int sequence) async {
    final sessionUserId = widget.hymnRepository.sessionUserId;
    if (sessionUserId == null || sessionUserId.isEmpty || assets.isEmpty) {
      return;
    }

    final resolvedPaths = <String, String>{};
    for (var i = 0; i < assets.length; i += _assetPrefetchBatchSize) {
      final batch = assets.skip(i).take(_assetPrefetchBatchSize).toList();
      final batchEntries = await Future.wait(
        batch.map(
          (asset) => _cacheAssetPath(asset, sessionUserId: sessionUserId),
        ),
      );

      if (!mounted || sequence != _assetLoadSequence) {
        return;
      }

      for (final entry in batchEntries.whereType<MapEntry<String, String>>()) {
        resolvedPaths[entry.key] = entry.value;
      }

      setState(() {
        _localAssetPaths
          ..clear()
          ..addAll(resolvedPaths);
      });
    }
  }

  Future<MapEntry<String, String>?> _cacheAssetPath(
    HymnAsset asset, {
    required String sessionUserId,
  }) async {
    final path = await _localAssetCache.getOrDownload(
      asset,
      userId: sessionUserId,
    );
    if (path == null) {
      return null;
    }
    return MapEntry(asset.id, path);
  }

  Future<void> _toggleFavorite() async {
    if (_togglingFavorite) {
      return;
    }

    setState(() {
      _togglingFavorite = true;
    });

    try {
      final favorite =
          await widget.hymnRepository.toggleFavorite(widget.hymnId);
      if (!mounted) {
        return;
      }
      setState(() {
        _favorite = favorite;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            favorite ? '즐겨찾기에 추가했어요.' : '즐겨찾기에서 삭제했어요.',
          ),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.message)),
      );
    } finally {
      if (mounted) {
        setState(() {
          _togglingFavorite = false;
        });
      }
    }
  }

  Future<void> _saveNote() async {
    if (_savingNote) {
      return;
    }

    final content = _noteController.text.trim();
    if (content.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('메모를 입력해 주세요.')),
      );
      return;
    }

    setState(() {
      _savingNote = true;
    });

    try {
      await widget.hymnRepository.saveNote(widget.hymnId, content);
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('메모를 저장했어요.')),
      );
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.message)),
      );
    } finally {
      if (mounted) {
        setState(() {
          _savingNote = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;

    return Scaffold(
      appBar: AppBar(
        title: Text(detail?.title ?? '찬양 상세'),
        actions: [
          IconButton(
            onPressed: _togglingFavorite ? null : _toggleFavorite,
            icon: Icon(_favorite ? Icons.favorite : Icons.favorite_border),
            tooltip: '즐겨찾기',
          ),
        ],
      ),
      body: _buildBody(detail),
    );
  }

  Widget _buildBody(HymnDetail? detail) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_error!, style: const TextStyle(color: Colors.red)),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: _load,
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      );
    }

    if (detail == null) {
      return const Center(child: Text('찬양 정보를 불러오지 못했습니다.'));
    }

    final imageAssets = detail.assets.where((asset) => asset.isImage).toList();
    final midiAssets = detail.assets.where((asset) => asset.isMidi).toList();

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
        children: [
          _SongSummaryCard(detail: detail),
          const SizedBox(height: 16),
          _SectionTitle(
            title: '악보',
            subtitle: imageAssets.isEmpty
                ? '등록된 악보가 없습니다.'
                : '총 ${imageAssets.length}페이지',
          ),
          const SizedBox(height: 8),
          if (imageAssets.isEmpty)
            const _EmptyCard(
              message: '관리자가 악보를 등록하면 여기에서 바로 볼 수 있어요.',
            )
          else
            ...List.generate(
              imageAssets.length,
              (index) => Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: _ImageAssetCard(
                  asset: imageAssets[index],
                  localPath: _localAssetPaths[imageAssets[index].id],
                  label: '악보 ${index + 1}페이지',
                ),
              ),
            ),
          const SizedBox(height: 10),
          _SectionTitle(
            title: '반주',
            subtitle: midiAssets.isEmpty
                ? '등록된 반주 파일이 없습니다.'
                : '총 ${midiAssets.length}개',
          ),
          const SizedBox(height: 8),
          if (midiAssets.isEmpty)
            const _EmptyCard(message: '등록된 반주 파일이 없습니다.')
          else
            ...List.generate(
              midiAssets.length,
              (index) => Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: _MidiCard(
                  title: '반주 ${index + 1}',
                  url: midiAssets[index].url,
                  localPath: _localAssetPaths[midiAssets[index].id],
                ),
              ),
            ),
          const SizedBox(height: 10),
          const _SectionTitle(
            title: '메모',
            subtitle: '개인 메모를 저장해 보세요.',
          ),
          const SizedBox(height: 8),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  TextField(
                    controller: _noteController,
                    minLines: 4,
                    maxLines: 8,
                    decoration: InputDecoration(
                      hintText: '이 찬양에 대한 메모를 남겨보세요.',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Align(
                    alignment: Alignment.centerRight,
                    child: FilledButton(
                      onPressed: _savingNote ? null : _saveNote,
                      child: Text(_savingNote ? '저장 중...' : '메모 저장'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SongSummaryCard extends StatelessWidget {
  final HymnDetail detail;

  const _SongSummaryCard({required this.detail});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              detail.title,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (detail.number != null && detail.number!.trim().isNotEmpty)
                  _Badge(text: '번호 ${detail.number}'),
                if (detail.tags != null && detail.tags!.trim().isNotEmpty)
                  _Badge(text: detail.tags!),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  final String text;

  const _Badge({required this.text});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: const Color(0xFFFFEEE1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        text,
        style: const TextStyle(
          color: Color(0xFFEA580C),
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final String subtitle;

  const _SectionTitle({
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 2),
        Text(
          subtitle,
          style: const TextStyle(color: Color(0xFF6B7280)),
        ),
      ],
    );
  }
}

class _EmptyCard extends StatelessWidget {
  final String message;

  const _EmptyCard({required this.message});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Text(
          message,
          style: const TextStyle(color: Color(0xFF6B7280)),
        ),
      ),
    );
  }
}

class _ImageAssetCard extends StatelessWidget {
  final HymnAsset asset;
  final String label;
  final String? localPath;

  const _ImageAssetCard({
    required this.asset,
    required this.localPath,
    required this.label,
  });

  bool get _hasLocalFile =>
      localPath != null &&
      localPath!.isNotEmpty &&
      File(localPath!).existsSync();

  bool get _hasRemoteUrl => asset.url.startsWith('http');

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            if (_hasLocalFile)
              _buildLocalImage()
            else if (_hasRemoteUrl)
              _buildRemoteImage()
            else
              const Text(
                '이미지 주소가 올바르지 않습니다.',
                style: TextStyle(color: Colors.red),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildLocalImage() {
    return _wrapZoomable(
      Image.file(
        File(localPath!),
        fit: BoxFit.fitWidth,
        errorBuilder: (_, __, ___) {
          if (_hasRemoteUrl) {
            return _buildRemoteImageContent();
          }
          return _buildImageLoadError();
        },
      ),
    );
  }

  Widget _buildRemoteImage() {
    return _wrapZoomable(_buildRemoteImageContent());
  }

  Widget _buildRemoteImageContent() {
    return Image.network(
      asset.url,
      fit: BoxFit.fitWidth,
      errorBuilder: (_, __, ___) => _buildImageLoadError(),
    );
  }

  Widget _wrapZoomable(Widget child) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(10),
      child: InteractiveViewer(
        minScale: 1,
        maxScale: 4,
        child: child,
      ),
    );
  }

  Widget _buildImageLoadError() {
    return const Padding(
      padding: EdgeInsets.all(12),
      child: Text('악보 이미지를 불러오지 못했습니다.'),
    );
  }
}

class _MidiCard extends StatelessWidget {
  final String title;
  final String url;
  final String? localPath;

  const _MidiCard({
    required this.title,
    required this.url,
    required this.localPath,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            _MidiAssetPlayer(
              url: url,
              localPath: localPath,
            ),
          ],
        ),
      ),
    );
  }
}

class _MidiAssetPlayer extends StatefulWidget {
  final String url;
  final String? localPath;

  const _MidiAssetPlayer({
    required this.url,
    required this.localPath,
  });

  @override
  State<_MidiAssetPlayer> createState() => _MidiAssetPlayerState();
}

class _MidiAssetPlayerState extends State<_MidiAssetPlayer> {
  late final AudioPlayer _player;
  PlayerState _state = PlayerState.stopped;
  double _speed = 1.0;
  String? _error;

  @override
  void initState() {
    super.initState();
    _player = AudioPlayer();
    _player.onPlayerStateChanged.listen((state) {
      if (!mounted) {
        return;
      }
      setState(() {
        _state = state;
      });
    });
  }

  @override
  void dispose() {
    _player.dispose();
    super.dispose();
  }

  Future<void> _playOrPause() async {
    try {
      if (_state == PlayerState.playing) {
        await _player.pause();
      } else if (_state == PlayerState.paused) {
        await _player.resume();
      } else {
        await _player.setPlaybackRate(_speed);
        await _playFromBestSource();
      }
      if (!mounted) {
        return;
      }
      setState(() {
        _error = null;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _error = '반주 재생에 실패했습니다.';
      });
    }
  }

  Future<void> _playFromBestSource() async {
    final filePath = widget.localPath;
    if (filePath != null &&
        filePath.isNotEmpty &&
        File(filePath).existsSync()) {
      try {
        await _player.play(DeviceFileSource(filePath));
        return;
      } catch (_) {
        // Fallback to network when local file cannot be read.
      }
    }
    await _player.play(UrlSource(widget.url));
  }

  Future<void> _stop() async {
    try {
      await _player.stop();
    } catch (_) {
      // 정지 실패는 사용 흐름을 막지 않는다.
    }
  }

  Future<void> _changeSpeed(double nextSpeed) async {
    setState(() {
      _speed = nextSpeed;
    });
    try {
      await _player.setPlaybackRate(nextSpeed);
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _error = '재생 속도 변경에 실패했습니다.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final playing = _state == PlayerState.playing;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 8,
          runSpacing: 8,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            FilledButton.icon(
              onPressed: _playOrPause,
              icon: Icon(playing ? Icons.pause : Icons.play_arrow),
              label: Text(playing ? '일시정지' : '재생'),
            ),
            OutlinedButton.icon(
              onPressed: _stop,
              icon: const Icon(Icons.stop),
              label: const Text('정지'),
            ),
            DropdownButton<double>(
              value: _speed,
              items: const [
                DropdownMenuItem(value: 0.75, child: Text('0.75배속')),
                DropdownMenuItem(value: 1.0, child: Text('1.0배속')),
                DropdownMenuItem(value: 1.25, child: Text('1.25배속')),
                DropdownMenuItem(value: 1.5, child: Text('1.5배속')),
              ],
              onChanged: (value) {
                if (value != null) {
                  _changeSpeed(value);
                }
              },
            ),
          ],
        ),
        if (_error != null) ...[
          const SizedBox(height: 6),
          Text(
            _error!,
            style: const TextStyle(color: Colors.red),
          ),
        ],
      ],
    );
  }
}
