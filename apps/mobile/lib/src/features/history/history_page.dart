import 'package:flutter/material.dart';

import '../hymn/hymn_repository.dart';

class HistoryPage extends StatefulWidget {
  final HymnRepository hymnRepository;
  final void Function(String hymnId) onOpenHymnDetail;

  const HistoryPage({
    super.key,
    required this.hymnRepository,
    required this.onOpenHymnDetail,
  });

  @override
  State<HistoryPage> createState() => _HistoryPageState();
}

class _HistoryPageState extends State<HistoryPage> {
  bool _loading = true;
  String? _error;
  List<HistoryItem> _items = const [];

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
      final items = await widget.hymnRepository.getHistory();
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
              FilledButton(onPressed: _load, child: const Text('다시 시도')),
            ],
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: CustomScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        slivers: [
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 10),
              child: Text(
                _items.isEmpty
                    ? '아직 열어본 찬양이 없어요.'
                    : '최근에 열어본 ${_items.length}곡',
                style: const TextStyle(
                  color: Color(0xFF5B6572),
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ),
          if (_items.isEmpty)
            const SliverFillRemaining(
              hasScrollBody: false,
              child: Center(
                child: Text(
                  '찬양을 열어보면 이곳에 기록됩니다.',
                  style: TextStyle(color: Color(0xFF6B7280)),
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
              sliver: SliverList.builder(
                itemCount: _items.length,
                itemBuilder: (context, index) {
                  final item = _items[index];
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _HistoryCard(
                      item: item,
                      onTap: () => widget.onOpenHymnDetail(item.id),
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

class _HistoryCard extends StatelessWidget {
  final HistoryItem item;
  final VoidCallback onTap;

  const _HistoryCard({
    required this.item,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final lastOpened = _formatRelativeTime(item.lastOpenedAt);
    final numberText = item.number?.trim();

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
                width: 42,
                height: 42,
                decoration: const BoxDecoration(
                  color: Color(0xFFFFEEE1),
                  shape: BoxShape.circle,
                ),
                child:
                    const Icon(Icons.history_rounded, color: Color(0xFFEA580C)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 15.5,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      numberText == null || numberText.isEmpty
                          ? lastOpened
                          : '$numberText · $lastOpened',
                      style: const TextStyle(
                        color: Color(0xFF6B7280),
                        fontSize: 12.5,
                      ),
                    ),
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

String _formatRelativeTime(String? value) {
  if (value == null || value.trim().isEmpty) {
    return '방금 전';
  }

  final parsed = DateTime.tryParse(value)?.toLocal();
  if (parsed == null) {
    return value;
  }

  final diff = DateTime.now().difference(parsed);
  if (diff.inMinutes < 1) {
    return '방금 전';
  }
  if (diff.inHours < 1) {
    return '${diff.inMinutes}분 전';
  }
  if (diff.inDays < 1) {
    return '${diff.inHours}시간 전';
  }
  if (diff.inDays < 30) {
    return '${diff.inDays}일 전';
  }

  final month = parsed.month.toString().padLeft(2, '0');
  final day = parsed.day.toString().padLeft(2, '0');
  return '${parsed.year}.$month.$day';
}
