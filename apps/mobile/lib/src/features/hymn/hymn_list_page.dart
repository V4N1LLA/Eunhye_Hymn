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
  final _searchController = TextEditingController();

  bool _loading = true;
  String? _error;
  List<HymnSummary> _items = const [];
  String _query = '';
  String? _selectedTag;

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

  void _clearFilters() {
    _searchController.clear();
    setState(() {
      _query = '';
      _selectedTag = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_loading && _items.isEmpty) {
      return const _HymnListSkeleton();
    }

    if (_error != null && _items.isEmpty) {
      return _LoadErrorView(message: _error!, onRetry: _load);
    }

    final filtered = _filterItems();
    final tags = _collectPopularTags(_items);
    final hasActiveFilter = _query.trim().isNotEmpty || _selectedTag != null;

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
                              tooltip: '검색어 지우기',
                            ),
                      filled: true,
                      fillColor: Colors.white,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                    ),
                  ),
                  if (tags.isNotEmpty) ...[
                    const SizedBox(height: 10),
                    SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: [
                          for (final tag in tags)
                            Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: ChoiceChip(
                                label: Text(tag),
                                selected: _selectedTag == tag,
                                onSelected: (selected) {
                                  setState(() {
                                    _selectedTag = selected ? tag : null;
                                  });
                                },
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          hasActiveFilter
                              ? '검색 결과 ${filtered.length}곡'
                              : '전체 ${_items.length}곡',
                          style: const TextStyle(
                            color: Color(0xFF5B6572),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      if (hasActiveFilter)
                        TextButton.icon(
                          onPressed: _clearFilters,
                          icon: const Icon(Icons.refresh, size: 18),
                          label: const Text('필터 초기화'),
                        ),
                    ],
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 8),
                    _SoftErrorBanner(
                      message: '네트워크가 불안정해요. 이전 데이터로 보여드리고 있어요.',
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
              child: _EmptyState(
                hasFilter: hasActiveFilter,
                onResetFilter: _clearFilters,
                onRefresh: () => _load(showLoading: false),
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
                      selectedTag: _selectedTag,
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

  List<HymnSummary> _filterItems() {
    final query = _query.trim().toLowerCase();
    final selectedTag = _selectedTag?.toLowerCase();

    return _items.where((item) {
      final textMatched = query.isEmpty ||
          item.title.toLowerCase().contains(query) ||
          (item.number ?? '').toLowerCase().contains(query) ||
          (item.tags ?? '').toLowerCase().contains(query);

      if (!textMatched) {
        return false;
      }

      if (selectedTag == null) {
        return true;
      }

      return _splitTags(item.tags)
          .any((tag) => tag.toLowerCase() == selectedTag);
    }).toList();
  }

  List<String> _collectPopularTags(List<HymnSummary> items) {
    final counts = <String, int>{};
    for (final item in items) {
      for (final tag in _splitTags(item.tags)) {
        counts[tag] = (counts[tag] ?? 0) + 1;
      }
    }

    final entries = counts.entries.toList()
      ..sort((a, b) {
        final byCount = b.value.compareTo(a.value);
        if (byCount != 0) {
          return byCount;
        }
        return a.key.compareTo(b.key);
      });

    return entries.take(8).map((entry) => entry.key).toList();
  }
}

class _HymnCard extends StatelessWidget {
  final HymnSummary hymn;
  final String? selectedTag;
  final VoidCallback onTap;

  const _HymnCard({
    required this.hymn,
    required this.selectedTag,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final tags = _splitTags(hymn.tags);
    final selectedTagLower = selectedTag?.toLowerCase();

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
                constraints: const BoxConstraints(minWidth: 56),
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
                    if (tags.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: [
                          for (final tag in tags.take(3))
                            _TagChip(
                              label: tag,
                              selected: selectedTagLower != null &&
                                  tag.toLowerCase() == selectedTagLower,
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
                child: Icon(
                  Icons.chevron_right_rounded,
                  color: Color(0xFF9CA3AF),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TagChip extends StatelessWidget {
  final String label;
  final bool selected;

  const _TagChip({
    required this.label,
    required this.selected,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: selected ? const Color(0xFFFFE0CC) : const Color(0xFFF3F4F6),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 12,
          color: selected ? const Color(0xFFEA580C) : const Color(0xFF6B7280),
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final bool hasFilter;
  final VoidCallback onResetFilter;
  final VoidCallback onRefresh;

  const _EmptyState({
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
            const Icon(Icons.search_off_rounded,
                size: 42, color: Color(0xFF9CA3AF)),
            const SizedBox(height: 10),
            Text(
              hasFilter ? '조건에 맞는 찬양이 없어요.' : '표시할 찬양이 아직 없어요.',
              style: const TextStyle(
                color: Color(0xFF374151),
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              hasFilter
                  ? '검색어나 태그 필터를 초기화하고 다시 확인해 보세요.'
                  : '잠시 후 새로고침해서 다시 확인해 보세요.',
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

class _LoadErrorView extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;

  const _LoadErrorView({
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
            FilledButton(
              onPressed: onRetry,
              child: const Text('다시 시도'),
            ),
          ],
        ),
      ),
    );
  }
}

class _SoftErrorBanner extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _SoftErrorBanner({
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

class _HymnListSkeleton extends StatelessWidget {
  const _HymnListSkeleton();

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      itemCount: 6,
      itemBuilder: (context, index) {
        return Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: Container(
            height: 88,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
            ),
          ),
        );
      },
    );
  }
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
