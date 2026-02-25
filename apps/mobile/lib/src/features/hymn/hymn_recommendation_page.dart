import 'package:flutter/material.dart';

import 'hymn_repository.dart';

class HymnRecommendationPage extends StatefulWidget {
  final HymnRepository hymnRepository;
  final Future<void> Function(String hymnId) onOpenHymnDetail;

  const HymnRecommendationPage({
    super.key,
    required this.hymnRepository,
    required this.onOpenHymnDetail,
  });

  @override
  State<HymnRecommendationPage> createState() => _HymnRecommendationPageState();
}

class _HymnRecommendationPageState extends State<HymnRecommendationPage> {
  final TextEditingController _situationController = TextEditingController();

  bool _loading = false;
  int _maxResults = 3;
  String? _error;
  HymnRecommendationResult? _result;

  @override
  void dispose() {
    _situationController.dispose();
    super.dispose();
  }

  Future<void> _recommend() async {
    final situation = _situationController.text.trim();
    if (situation.isEmpty) {
      setState(() {
        _error = '상황 설명을 먼저 입력해 주세요.';
      });
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final result = await widget.hymnRepository.recommendHymns(
        situation: situation,
        maxResults: _maxResults,
      );
      if (!mounted) {
        return;
      }
      setState(() {
        _result = result;
      });
    } catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _result = null;
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
    final result = _result;

    return Scaffold(
      appBar: AppBar(
        title: const Text('AI 찬송 추천'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '상황을 입력하면 추천 찬송을 찾아드립니다.',
                    style: TextStyle(
                      fontSize: 14,
                      color: Color(0xFF4B5563),
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _situationController,
                    maxLength: 180,
                    minLines: 3,
                    maxLines: 5,
                    decoration: const InputDecoration(
                      hintText: '예: 새벽 말씀, 차분하고 묵상 분위기',
                    ),
                  ),
                  const SizedBox(height: 10),
                  const Text(
                    '추천 개수',
                    style: TextStyle(
                      color: Color(0xFF374151),
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [
                      for (final value in const [1, 2, 3, 4, 5])
                        ChoiceChip(
                          label: Text(value.toString()),
                          selected: _maxResults == value,
                          onSelected: (selected) {
                            if (!selected) {
                              return;
                            }
                            setState(() {
                              _maxResults = value;
                            });
                          },
                        ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: _loading ? null : _recommend,
                    icon: _loading
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.auto_awesome),
                    label: Text(_loading ? '추천 중...' : 'AI 추천 실행'),
                  ),
                ],
              ),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Container(
              decoration: BoxDecoration(
                color: const Color(0xFFFEF2F2),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFFFECACA)),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              child: Text(
                _error!,
                style: const TextStyle(
                  color: Color(0xFFB91C1C),
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
          if (result != null) ...[
            const SizedBox(height: 12),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  children: [
                    Expanded(
                      child: _MetricChip(
                        label: '요청 개수',
                        value: result.requestedMaxResults.toString(),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _MetricChip(
                        label: '검색 후보',
                        value: result.candidateCount.toString(),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _MetricChip(
                        label: '추천 결과',
                        value: result.items.length.toString(),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            if (result.items.isEmpty)
              const Card(
                child: Padding(
                  padding: EdgeInsets.all(14),
                  child: Text(
                    '추천 결과가 없습니다. 상황 설명을 조금 더 구체적으로 입력해 보세요.',
                    style: TextStyle(color: Color(0xFF4B5563)),
                  ),
                ),
              )
            else
              for (final item in result.items)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Card(
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 10,
                                  vertical: 6,
                                ),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFFFEEE1),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Text(
                                  item.number?.trim().isNotEmpty == true
                                      ? item.number!
                                      : '-',
                                  style: const TextStyle(
                                    color: Color(0xFFEA580C),
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      item.title,
                                      style: const TextStyle(
                                        fontSize: 15.5,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      item.tags?.trim().isNotEmpty == true
                                          ? item.tags!
                                          : '태그 없음',
                                      style: const TextStyle(
                                        color: Color(0xFF6B7280),
                                        fontSize: 12.5,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              IconButton(
                                onPressed: () =>
                                    widget.onOpenHymnDetail(item.id),
                                icon: const Icon(Icons.chevron_right),
                                tooltip: '상세 보기',
                              ),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Container(
                            width: double.infinity,
                            decoration: BoxDecoration(
                              color: const Color(0xFFF8FAFC),
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(
                                color: const Color(0xFFE5E7EB),
                              ),
                            ),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 8,
                            ),
                            child: Text(
                              item.reason,
                              style: const TextStyle(
                                color: Color(0xFF374151),
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
          ],
        ],
      ),
    );
  }
}

class _MetricChip extends StatelessWidget {
  final String label;
  final String value;

  const _MetricChip({
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFE5E7EB)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(
              color: Color(0xFF6B7280),
              fontSize: 11.5,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            value,
            style: const TextStyle(
              color: Color(0xFF111827),
              fontSize: 15,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}
