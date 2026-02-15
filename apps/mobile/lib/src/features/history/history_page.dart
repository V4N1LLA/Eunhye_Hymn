import 'package:flutter/material.dart';

import '../hymn/hymn_repository.dart';

enum _HistoryRange { all, today, week, month }

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
  final _searchController = TextEditingController();

  bool _loading = true;
  String? _error;
  List<HistoryItem> _items = const [];
  String _query = '';
  _HistoryRange _range = _HistoryRange.all;

  @override
  void initState() {
    super.initState();
    _searchController.addListener(() {
      final next = _searchController.text;
      if (_query == next) {
        return;
      }
      setState(() {
        _query = next;
      });
    });
    _load();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _load({bool showLoading = true}) async {
    if (showLoading || _items.isEmpty) {
      setState(() {
        _loading = true;
        _error = null;
      });
    } else {
      setState(() {
        _error = null;
      });
    }

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

  void _clearFilters() {
    _searchController.clear();
    setState(() {
      _query = '';
      _range = _HistoryRange.all;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_loading && _items.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null && _items.isEmpty) {
      return _HistoryErrorView(message: _error!, onRetry: _load);
    }

    final filtered = _filterItems();
    final hasFilter = _query.trim().isNotEmpty || _range != _HistoryRange.all;

    return RefreshIndicator(
      onRefresh: () => _load(showLoading: false),
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
                    controller: _searchController,
                    decoration: InputDecoration(
                      hintText: '제목, 번호, 태그 검색',
                      prefixIcon: const Icon(Icons.search),
                      suffixIcon: _query.trim().isEmpty
                          ? null
                          : IconButton(
                              onPressed: _searchController.clear,
                              icon: const Icon(Icons.close),
                            ),
                      filled: true,
                      fillColor: Colors.white,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final range in _HistoryRange.values)
                        ChoiceChip(
                          label: Text(_labelForRange(range)),
                          selected: _range == range,
                          onSelected: (selected) {
                            if (!selected) {
                              return;
                            }
                            setState(() {
                              _range = range;
                            });
                          },
                        ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          filtered.isEmpty
                              ? '표시할 최근 기록이 없어요.'
                              : '최근 열람 ${filtered.length}곡',
                          style: const TextStyle(
                            color: Color(0xFF5B6572),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      if (hasFilter)
                        TextButton.icon(
                          onPressed: _clearFilters,
                          icon: const Icon(Icons.filter_alt_off, size: 18),
                          label: const Text('필터 초기화'),
                        ),
                    ],
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 8),
                    _HistorySoftErrorBanner(
                      message: '연결이 불안정해요. 저장된 기록으로 보여드리고 있어요.',
                      onRetry: () => _load(showLoading: false),
                    ),
                  ],
                ],
              ),
            ),
          ),
          if (filtered.isEmpty)
            SliverFillRemaining(
              hasScrollBody: false,
              child: _HistoryEmptyState(
                hasFilter: hasFilter,
                onResetFilter: _clearFilters,
                onRefresh: () => _load(showLoading: false),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
              sliver: SliverList.builder(
                itemCount: filtered.length,
                itemBuilder: (context, index) {
                  final item = filtered[index];
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

  List<HistoryItem> _filterItems() {
    final now = DateTime.now();
    final query = _query.trim().toLowerCase();

    final filtered = _items.where((item) {
      final itemDate = _tryParseDate(item.lastOpenedAt);
      if (!_matchRange(_range, now, itemDate)) {
        return false;
      }

      if (query.isEmpty) {
        return true;
      }

      return item.title.toLowerCase().contains(query) ||
          (item.number ?? '').toLowerCase().contains(query) ||
          (item.tags ?? '').toLowerCase().contains(query);
    }).toList();

    filtered.sort((a, b) {
      final left = _tryParseDate(a.lastOpenedAt);
      final right = _tryParseDate(b.lastOpenedAt);
      if (left == null && right == null) {
        return 0;
      }
      if (left == null) {
        return 1;
      }
      if (right == null) {
        return -1;
      }
      return right.compareTo(left);
    });

    return filtered;
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
    final tags = _splitTags(item.tags);

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
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
                    if (tags.isNotEmpty) ...[
                      const SizedBox(height: 7),
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: [
                          for (final tag in tags.take(3))
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 8, vertical: 3),
                              decoration: BoxDecoration(
                                color: const Color(0xFFF3F4F6),
                                borderRadius: BorderRadius.circular(999),
                              ),
                              child: Text(
                                tag,
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: Color(0xFF6B7280),
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 6),
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child:
                    Icon(Icons.chevron_right_rounded, color: Color(0xFF9CA3AF)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HistoryEmptyState extends StatelessWidget {
  final bool hasFilter;
  final VoidCallback onResetFilter;
  final VoidCallback onRefresh;

  const _HistoryEmptyState({
    required this.hasFilter,
    required this.onResetFilter,
    required this.onRefresh,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.history_toggle_off_rounded,
                size: 42, color: Color(0xFF9CA3AF)),
            const SizedBox(height: 10),
            Text(
              hasFilter ? '조건에 맞는 기록이 없어요.' : '아직 최근 열람 기록이 없어요.',
              style: const TextStyle(
                color: Color(0xFF374151),
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              hasFilter ? '필터를 초기화하고 다른 찬양을 찾아보세요.' : '찬양을 열어보면 이곳에 자동으로 기록돼요.',
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF6B7280)),
            ),
            const SizedBox(height: 14),
            if (hasFilter)
              FilledButton.tonalIcon(
                onPressed: onResetFilter,
                icon: const Icon(Icons.filter_alt_off),
                label: const Text('필터 초기화'),
              )
            else
              FilledButton.tonalIcon(
                onPressed: onRefresh,
                icon: const Icon(Icons.refresh),
                label: const Text('새로고침'),
              ),
          ],
        ),
      ),
    );
  }
}

class _HistoryErrorView extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;

  const _HistoryErrorView({
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_rounded, size: 40, color: Colors.red),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.red),
            ),
            const SizedBox(height: 12),
            FilledButton(onPressed: onRetry, child: const Text('다시 시도')),
          ],
        ),
      ),
    );
  }
}

class _HistorySoftErrorBanner extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _HistorySoftErrorBanner({
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBEB),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFFDE68A)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      child: Row(
        children: [
          const Icon(Icons.wifi_off_rounded,
              size: 18, color: Color(0xFF92400E)),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(color: Color(0xFF92400E)),
            ),
          ),
          TextButton(onPressed: onRetry, child: const Text('재시도')),
        ],
      ),
    );
  }
}

String _labelForRange(_HistoryRange range) {
  switch (range) {
    case _HistoryRange.all:
      return '전체';
    case _HistoryRange.today:
      return '오늘';
    case _HistoryRange.week:
      return '최근 7일';
    case _HistoryRange.month:
      return '최근 30일';
  }
}

bool _matchRange(_HistoryRange range, DateTime now, DateTime? itemDate) {
  if (range == _HistoryRange.all || itemDate == null) {
    return true;
  }

  final diff = now.difference(itemDate);
  if (diff.isNegative) {
    return true;
  }

  switch (range) {
    case _HistoryRange.all:
      return true;
    case _HistoryRange.today:
      return now.year == itemDate.year &&
          now.month == itemDate.month &&
          now.day == itemDate.day;
    case _HistoryRange.week:
      return diff.inDays < 7;
    case _HistoryRange.month:
      return diff.inDays < 30;
  }
}

DateTime? _tryParseDate(String? value) {
  if (value == null || value.trim().isEmpty) {
    return null;
  }
  return DateTime.tryParse(value)?.toLocal();
}

String _formatRelativeTime(String? value) {
  final parsed = _tryParseDate(value);
  if (parsed == null) {
    return '방금 전';
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

List<String> _splitTags(String? raw) {
  if (raw == null || raw.trim().isEmpty) {
    return const [];
  }

  return raw
      .split(',')
      .map((tag) => tag.trim())
      .where((tag) => tag.isNotEmpty)
      .toList();
}
