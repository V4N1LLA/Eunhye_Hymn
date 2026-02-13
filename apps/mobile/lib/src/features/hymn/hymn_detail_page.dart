import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import 'hymn_repository.dart';

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
  final _noteController = TextEditingController();

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
      setState(() {
        _detail = detail;
        _noteController.text = note ?? '';
        _favorite = favorite;
      });
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

  Future<void> _toggleFavorite() async {
    if (_togglingFavorite) {
      return;
    }

    setState(() {
      _togglingFavorite = true;
    });

    try {
      final favorite = await widget.hymnRepository.toggleFavorite(widget.hymnId);
      if (!mounted) {
        return;
      }
      setState(() {
        _favorite = favorite;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(favorite ? '즐겨찾기에 추가했습니다.' : '즐겨찾기를 해제했습니다.'),
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
        const SnackBar(content: Text('메모를 입력한 뒤 저장하세요.')),
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
        const SnackBar(content: Text('메모를 저장했습니다.')),
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
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: const Text('찬양 상세')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (_error != null) {
      return Scaffold(
        appBar: AppBar(title: const Text('찬양 상세')),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(16),
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
        ),
      );
    }

    final detail = _detail;
    if (detail == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('찬양 상세')),
        body: const Center(child: Text('데이터가 없습니다.')),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(detail.title),
        actions: [
          IconButton(
            onPressed: _togglingFavorite ? null : _toggleFavorite,
            icon: Icon(_favorite ? Icons.favorite : Icons.favorite_border),
            tooltip: '즐겨찾기',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    detail.title,
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 8),
                  Text('번호: ${detail.number ?? "-"}'),
                  Text('태그: ${detail.tags ?? "-"}'),
                  Text('활성화: ${detail.enabled ? "예" : "아니오"}'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            '에셋',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 8),
          if (detail.assets.isEmpty)
            const Text('등록된 에셋이 없습니다.')
          else
            ...detail.assets.map((asset) => _AssetCard(asset: asset)),
          const SizedBox(height: 20),
          Text(
            '메모',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _noteController,
            minLines: 4,
            maxLines: 8,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              hintText: '찬양 메모를 입력하세요.',
            ),
          ),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton(
              onPressed: _savingNote ? null : _saveNote,
              child: Text(_savingNote ? '저장 중...' : '메모 저장'),
            ),
          ),
        ],
      ),
    );
  }
}

class _AssetCard extends StatelessWidget {
  final HymnAsset asset;

  const _AssetCard({required this.asset});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('타입: ${asset.type} / 파트: ${asset.part ?? "-"}'),
            const SizedBox(height: 6),
            if (asset.isImage && asset.url.startsWith('http'))
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.network(
                  asset.url,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => const Padding(
                    padding: EdgeInsets.all(12),
                    child: Text('이미지를 불러올 수 없습니다.'),
                  ),
                ),
              )
            else
              SelectableText(asset.url),
          ],
        ),
      ),
    );
  }
}
