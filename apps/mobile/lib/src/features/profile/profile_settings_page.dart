import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import '../../core/storage/onboarding_storage.dart';
import '../auth/auth_repository.dart';
import '../hymn/hymn_repository.dart';
import '../hymn/local_asset_cache.dart';

class ProfileSettingsPage extends StatefulWidget {
  final SessionProfile profile;
  final AuthRepository authRepository;
  final HymnRepository hymnRepository;
  final OnboardingStorage onboardingStorage;
  final ValueChanged<SessionProfile> onProfileUpdated;
  final Future<void> Function() onLogout;

  const ProfileSettingsPage({
    super.key,
    required this.profile,
    required this.authRepository,
    required this.hymnRepository,
    required this.onboardingStorage,
    required this.onProfileUpdated,
    required this.onLogout,
  });

  @override
  State<ProfileSettingsPage> createState() => _ProfileSettingsPageState();
}

class _ProfileSettingsPageState extends State<ProfileSettingsPage> {
  bool _loggingOut = false;

  Future<void> _refreshProfile() async {
    try {
      final updated = await widget.authRepository.fetchProfile();
      if (updated != null) {
        widget.onProfileUpdated(updated);
      }
    } catch (_) {
      // Keep current state on refresh failure.
    }
  }

  Future<void> _logout() async {
    if (_loggingOut) {
      return;
    }

    final confirmed = await showDialog<bool>(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('로그아웃'),
            content: const Text('로그아웃 하시겠어요?'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(false),
                child: const Text('취소'),
              ),
              FilledButton(
                onPressed: () => Navigator.of(context).pop(true),
                child: const Text('로그아웃'),
              ),
            ],
          ),
        ) ??
        false;

    if (!confirmed) {
      return;
    }

    setState(() {
      _loggingOut = true;
    });
    try {
      await widget.onLogout();
    } finally {
      if (mounted) {
        setState(() {
          _loggingOut = false;
        });
      }
    }
  }

  Future<void> _openProfileChangeRequestPage() async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => _ProfileChangeRequestPage(
          profile: widget.profile,
          authRepository: widget.authRepository,
        ),
      ),
    );
    await _refreshProfile();
  }

  Future<void> _openStoragePage() async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => _StorageManagementPage(
          profile: widget.profile,
          hymnRepository: widget.hymnRepository,
        ),
      ),
    );
  }

  Future<void> _openSupportPage() async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => const _SupportPage(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _refreshProfile,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
        children: [
          const Text(
            '설정',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            '원하는 항목을 눌러서 편하게 관리하세요.',
            style: TextStyle(
              color: Color(0xFF5B6572),
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 16),
          _MenuCard(
            title: '개인정보 변경 요청',
            subtitle: '교회, 이름, 구역, 성별 변경을 요청해요',
            icon: Icons.badge_outlined,
            onTap: _openProfileChangeRequestPage,
          ),
          const SizedBox(height: 10),
          _MenuCard(
            title: '저장공간 정리',
            subtitle: '앱이 느리거나 저장공간이 부족할 때 정리해요',
            icon: Icons.cleaning_services_outlined,
            onTap: _openStoragePage,
          ),
          const SizedBox(height: 10),
          _MenuCard(
            title: '후원 / 구독',
            subtitle: '후원과 정기 구독 안내를 확인해요',
            icon: Icons.volunteer_activism_outlined,
            onTap: _openSupportPage,
          ),
          const SizedBox(height: 10),
          _MenuCard(
            title: '로그아웃',
            subtitle: '현재 계정에서 로그아웃해요',
            icon: Icons.logout,
            danger: true,
            trailing: _loggingOut
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : null,
            onTap: _loggingOut ? null : _logout,
          ),
        ],
      ),
    );
  }
}

class _MenuCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final bool danger;
  final Widget? trailing;
  final VoidCallback? onTap;

  const _MenuCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.onTap,
    this.danger = false,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    final titleColor =
        danger ? const Color(0xFFB42318) : const Color(0xFF111827);
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Icon(icon, color: titleColor),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 16,
                        color: titleColor,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        color: Color(0xFF5B6572),
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
              ),
              trailing ??
                  const Icon(
                    Icons.chevron_right,
                    color: Color(0xFF94A3B8),
                  ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ProfileChangeRequestPage extends StatefulWidget {
  final SessionProfile profile;
  final AuthRepository authRepository;

  const _ProfileChangeRequestPage({
    required this.profile,
    required this.authRepository,
  });

  @override
  State<_ProfileChangeRequestPage> createState() =>
      _ProfileChangeRequestPageState();
}

class _ProfileChangeRequestPageState extends State<_ProfileChangeRequestPage> {
  final _churchController = TextEditingController();
  final _nameController = TextEditingController();
  final _groupController = TextEditingController();

  UserGender _selectedGender = UserGender.unknown;
  bool _loadingStatus = true;
  bool _submitting = false;
  String? _error;
  ProfileChangeRequestResult? _latest;

  @override
  void initState() {
    super.initState();
    _churchController.text = widget.profile.churchName ?? '';
    _nameController.text = widget.profile.name ?? '';
    _groupController.text = widget.profile.group ?? '';
    _selectedGender = widget.profile.gender;
    _loadLatest();
  }

  @override
  void dispose() {
    _churchController.dispose();
    _nameController.dispose();
    _groupController.dispose();
    super.dispose();
  }

  Future<void> _loadLatest() async {
    setState(() {
      _loadingStatus = true;
    });
    try {
      final latest =
          await widget.authRepository.fetchLatestProfileChangeRequest();
      if (!mounted) {
        return;
      }
      setState(() {
        _latest = latest;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _latest = null;
      });
    } finally {
      if (mounted) {
        setState(() {
          _loadingStatus = false;
        });
      }
    }
  }

  Future<void> _submitRequest() async {
    if (_submitting) {
      return;
    }

    final churchName = _churchController.text.trim();
    final name = _nameController.text.trim();
    final group = _groupController.text.trim();
    if (churchName.isEmpty || name.isEmpty || group.isEmpty) {
      setState(() {
        _error = '교회, 이름, 구역을 모두 입력해 주세요.';
      });
      return;
    }

    if (_selectedGender == UserGender.unknown) {
      setState(() {
        _error = '성별을 선택해 주세요.';
      });
      return;
    }

    setState(() {
      _submitting = true;
      _error = null;
    });

    try {
      final result = await widget.authRepository.requestProfileChange(
        churchName: churchName,
        name: name,
        group: group,
        gender: _selectedGender,
      );

      if (!mounted) {
        return;
      }
      setState(() {
        _latest = result;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('변경 요청을 보냈어요. 관리자 확인 후 반영됩니다.'),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _error = e.message;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _error = '요청을 보내는 중 문제가 생겼어요. 다시 시도해 주세요.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _submitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: const BackButton(),
        title: const Text('개인정보 변경 요청'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
        children: [
          const Text(
            '바뀐 정보를 입력한 뒤 요청을 보내면\n관리자가 확인 후 반영해요.',
            style: TextStyle(
              color: Color(0xFF5B6572),
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 14),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: _loadingStatus
                  ? const SizedBox(
                      height: 24,
                      child: Center(child: CircularProgressIndicator()),
                    )
                  : _ProfileRequestStatusView(result: _latest),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _churchController,
            enabled: !_submitting,
            decoration: const InputDecoration(
              labelText: '교회',
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _nameController,
            enabled: !_submitting,
            decoration: const InputDecoration(
              labelText: '이름',
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _groupController,
            enabled: !_submitting,
            decoration: const InputDecoration(
              labelText: '구역',
            ),
          ),
          const SizedBox(height: 10),
          InputDecorator(
            decoration: const InputDecoration(
              labelText: '성별',
            ),
            child: SegmentedButton<UserGender>(
              showSelectedIcon: false,
              selected: {_selectedGender},
              onSelectionChanged: _submitting
                  ? null
                  : (selection) {
                      if (selection.isEmpty) {
                        return;
                      }
                      setState(() {
                        _selectedGender = selection.first;
                      });
                    },
              segments: const [
                ButtonSegment<UserGender>(
                  value: UserGender.unknown,
                  label: Text('미선택'),
                ),
                ButtonSegment<UserGender>(
                  value: UserGender.male,
                  label: Text('남성'),
                ),
                ButtonSegment<UserGender>(
                  value: UserGender.female,
                  label: Text('여성'),
                ),
              ],
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 10),
            Text(
              _error!,
              style: const TextStyle(
                color: Color(0xFFC2291E),
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
          const SizedBox(height: 14),
          FilledButton(
            onPressed: _submitting ? null : _submitRequest,
            child: Text(_submitting ? '요청 보내는 중...' : '변경 요청 보내기'),
          ),
        ],
      ),
    );
  }
}

class _ProfileRequestStatusView extends StatelessWidget {
  final ProfileChangeRequestResult? result;

  const _ProfileRequestStatusView({required this.result});

  @override
  Widget build(BuildContext context) {
    if (result == null) {
      return const Text(
        '아직 보낸 요청이 없어요.',
        style: TextStyle(color: Color(0xFF5B6572)),
      );
    }

    final (title, color) = switch (result!.status) {
      ProfileChangeRequestStatus.pending => (
          '승인 대기 중',
          const Color(0xFF92400E)
        ),
      ProfileChangeRequestStatus.approved => ('승인 완료', const Color(0xFF166534)),
      ProfileChangeRequestStatus.rejected => ('반려됨', const Color(0xFFB42318)),
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: color.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(999),
              ),
              child: Text(
                title,
                style: TextStyle(
                  color: color,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            const SizedBox(width: 8),
            Text(
              '요청: ${_formatTime(result!.requestedAt)}',
              style: const TextStyle(fontSize: 12, color: Color(0xFF6B7280)),
            ),
          ],
        ),
        if (result!.reviewedAt != null) ...[
          const SizedBox(height: 6),
          Text(
            '처리: ${_formatTime(result!.reviewedAt!)}',
            style: const TextStyle(fontSize: 12, color: Color(0xFF6B7280)),
          ),
        ],
        const SizedBox(height: 4),
        Text(
          '성별: ${_genderLabel(result!.gender)}',
          style: const TextStyle(fontSize: 12, color: Color(0xFF6B7280)),
        ),
        if (result!.rejectReason != null &&
            result!.rejectReason!.trim().isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(
              '사유: ${result!.rejectReason!.trim()}',
              style: const TextStyle(
                fontSize: 12,
                color: Color(0xFFB42318),
              ),
            ),
          ),
      ],
    );
  }

  static String _formatTime(DateTime value) {
    final local = value.toLocal();
    final month = local.month.toString().padLeft(2, '0');
    final day = local.day.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '${local.year}-$month-$day $hour:$minute';
  }

  static String _genderLabel(UserGender gender) {
    return switch (gender) {
      UserGender.male => '남성',
      UserGender.female => '여성',
      UserGender.unknown => '미입력',
    };
  }
}

class _StorageManagementPage extends StatefulWidget {
  final SessionProfile profile;
  final HymnRepository hymnRepository;

  const _StorageManagementPage({
    required this.profile,
    required this.hymnRepository,
  });

  @override
  State<_StorageManagementPage> createState() => _StorageManagementPageState();
}

class _StorageManagementPageState extends State<_StorageManagementPage> {
  bool _clearing = false;

  Future<void> _clearStorage() async {
    if (_clearing) {
      return;
    }

    setState(() {
      _clearing = true;
    });

    try {
      await widget.hymnRepository.clearPersonalCache();
      final localAssetCache = LocalAssetCache();
      try {
        await localAssetCache.clearForUser(userId: widget.profile.userId);
      } finally {
        localAssetCache.dispose();
      }

      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('앱에 쌓인 임시 데이터를 정리했어요.')),
      );
    } catch (_) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('정리 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.')),
      );
    } finally {
      if (mounted) {
        setState(() {
          _clearing = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: const BackButton(),
        title: const Text('저장공간 정리'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
        children: [
          const Text(
            '앱이 느려지거나 저장공간이 부족할 때\n임시로 저장된 데이터를 정리해 보세요.',
            style: TextStyle(
              color: Color(0xFF5B6572),
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '임시 데이터 정리',
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 16,
                    ),
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    '다운로드된 악보/음원 임시 파일과 앱 캐시를 정리해요.\n로그인 정보는 그대로 유지됩니다.',
                    style: TextStyle(
                      color: Color(0xFF5B6572),
                    ),
                  ),
                  const SizedBox(height: 12),
                  FilledButton.tonalIcon(
                    onPressed: _clearing ? null : _clearStorage,
                    icon: _clearing
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.cleaning_services_outlined),
                    label: Text(_clearing ? '정리 중...' : '임시 데이터 정리하기'),
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

class _SupportPage extends StatelessWidget {
  const _SupportPage();

  Future<void> _showComingSoon(BuildContext context, String title) async {
    await showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: const Text(
          '이 기능은 준비 중입니다.\n곧 앱에서 바로 후원/구독할 수 있게 만들겠습니다.',
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: const BackButton(),
        title: const Text('후원 / 구독'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
        children: [
          const Text(
            '은혜찬송 사역을 위한 후원/구독 메뉴입니다.',
            style: TextStyle(
              color: Color(0xFF5B6572),
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '일시 후원',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 6),
                  const Text('원하는 금액으로 한 번 후원할 수 있어요.'),
                  const SizedBox(height: 10),
                  FilledButton.tonal(
                    onPressed: () => _showComingSoon(context, '일시 후원'),
                    child: const Text('일시 후원 준비 중'),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 10),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '정기 구독',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 6),
                  const Text('매달 자동 후원으로 사역을 꾸준히 도울 수 있어요.'),
                  const SizedBox(height: 10),
                  FilledButton.tonal(
                    onPressed: () => _showComingSoon(context, '정기 구독'),
                    child: const Text('정기 구독 준비 중'),
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
