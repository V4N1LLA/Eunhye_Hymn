import 'package:flutter/material.dart';

import 'hymn_repository.dart';

class HymnListPage extends StatefulWidget {
  final HymnRepository hymnRepository;
  final void Function(String hymnId) onOpenHymnDetail;

  const HymnListPage({
    super.key,
    required this.hymnRepository,
    required this.onOpenHymnDetail,
  });

  @override
  State<HymnListPage> createState() => _HymnListPageState();
}

class _HymnListPageState extends State<HymnListPage> {
  bool _loading = true;
  String? _error;
  List<HymnSummary> _items = const [];
  String _query = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final items = await widget.hymnRepository.listHymns();
      if (!mounted) {
        return;
      }
      setState(() {
        _items = items;
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

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
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
      );
    }

    final filtered = _items.where((item) {
      final q = _query.trim().toLowerCase();
      if (q.isEmpty) {
        return true;
      }
      return item.title.toLowerCase().contains(q) ||
          (item.number ?? '').toLowerCase().contains(q) ||
          (item.tags ?? '').toLowerCase().contains(q);
    }).toList();

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: TextField(
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              hintText: '제목, 번호, 태그 검색',
              prefixIcon: Icon(Icons.search),
            ),
            onChanged: (value) {
              setState(() {
                _query = value;
              });
            },
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: _load,
            child: filtered.isEmpty
                ? ListView(
                    children: const [
                      SizedBox(height: 80),
                      Center(child: Text('검색 결과가 없습니다.')),
                    ],
                  )
                : ListView.separated(
                    itemCount: filtered.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final hymn = filtered[index];
                      final subtitle = [
                        if (hymn.number != null && hymn.number!.isNotEmpty) '#${hymn.number}',
                        if (hymn.tags != null && hymn.tags!.isNotEmpty) hymn.tags,
                      ].join(' · ');
                      return ListTile(
                        title: Text(hymn.title),
                        subtitle: subtitle.isEmpty ? null : Text(subtitle),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => widget.onOpenHymnDetail(hymn.id),
                      );
                    },
                  ),
          ),
        ),
      ],
    );
  }
}
