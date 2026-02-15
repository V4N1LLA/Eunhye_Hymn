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

    final query = _query.trim().toLowerCase();
    final filtered = _items.where((item) {
      if (query.isEmpty) {
        return true;
      }
      return item.title.toLowerCase().contains(query) ||
          (item.number ?? '').toLowerCase().contains(query) ||
          (item.tags ?? '').toLowerCase().contains(query);
    }).toList();

    return RefreshIndicator(
      onRefresh: _load,
      child: CustomScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        slivers: [
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  TextField(
                    decoration: InputDecoration(
                      hintText: '제목, 번호, 태그 검색',
                      prefixIcon: const Icon(Icons.search),
                      filled: true,
                      fillColor: Colors.white,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                    ),
                    onChanged: (value) {
                      setState(() {
                        _query = value;
                      });
                    },
                  ),
                  const SizedBox(height: 10),
                  Text(
                    query.isEmpty
                        ? '전체 ${_items.length}곡'
                        : '검색 결과 ${filtered.length}곡',
                    style: const TextStyle(
                      color: Color(0xFF5B6572),
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
          ),
          if (filtered.isEmpty)
            const SliverFillRemaining(
              hasScrollBody: false,
              child: Center(
                child: Text(
                  '조건에 맞는 찬양이 없어요.',
                  style: TextStyle(color: Color(0xFF6B7280)),
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
              sliver: SliverList.builder(
                itemCount: filtered.length,
                itemBuilder: (context, index) {
                  final hymn = filtered[index];
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _HymnCard(
                      hymn: hymn,
                      onTap: () => widget.onOpenHymnDetail(hymn.id),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}

class _HymnCard extends StatelessWidget {
  final HymnSummary hymn;
  final VoidCallback onTap;

  const _HymnCard({
    required this.hymn,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Container(
                constraints: const BoxConstraints(minWidth: 54),
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFEEE1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  hymn.number?.isNotEmpty == true ? hymn.number! : '찬양',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Color(0xFFEA580C),
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      hymn.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 15.5,
                      ),
                    ),
                    if (hymn.tags != null && hymn.tags!.trim().isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Text(
                        hymn.tags!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: Color(0xFF6B7280),
                          fontSize: 12.5,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 6),
              const Icon(Icons.chevron_right_rounded, color: Color(0xFF9CA3AF)),
            ],
          ),
        ),
      ),
    );
  }
}
