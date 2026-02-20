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
  final _churchController = TextEditingController();
  final _nameController = TextEditingController();
  final _groupController = TextEditingController();

  bool _savingProfile = false;
  bool _clearingCache = false;
  bool _loggingOut = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _applyProfile(widget.profile);
  }

  @override
  void didUpdateWidget(covariant ProfileSettingsPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.profile != widget.profile) {
      _applyProfile(widget.profile);
    }
  }

  @override
  void dispose() {
    _churchController.dispose();
    _nameController.dispose();
    _groupController.dispose();
    super.dispose();
  }

  void _applyProfile(SessionProfile profile) {
    _churchController.text = profile.churchName ?? '';
    _nameController.text = profile.name ?? '';
    _groupController.text = profile.group ?? '';
  }

  Future<void> _saveProfile() async {
    final churchName = _churchController.text.trim();
    final name = _nameController.text.trim();
    final group = _groupController.text.trim();
    if (churchName.isEmpty || name.isEmpty || group.isEmpty) {
      setState(() {
        _error = '교회, 이름, 구역을 모두 입력해 주세요.';
      });
      return;
    }

    setState(() {
      _savingProfile = true;
      _error = null;
    });

    try {
      final updated = await widget.authRepository.updateProfile(
        churchName: churchName,
        name: name,
        group: group,
      );
      await widget.onboardingStorage.saveProfile(
        userId: updated.userId,
        churchName: churchName,
        name: name,
        group: group,
      );
      widget.onProfileUpdated(updated);
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('프로필을 저장했어요.')),
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
        _error = '프로필 저장 중 문제가 발생했습니다.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _savingProfile = false;
        });
      }
    }
  }

  Future<void> _clearCache() async {
    if (_clearingCache) {
      return;
    }

    setState(() {
      _clearingCache = true;
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
        const SnackBar(content: Text('개인 캐시를 정리했어요.')),
      );
    } catch (e) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('캐시 정리에 실패했습니다: $e')),
      );
    } finally {
      if (mounted) {
        setState(() {
          _clearingCache = false;
        });
      }
    }
  }

  Future<void> _logout() async {
    if (_loggingOut) {
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

  @override
  Widget build(BuildContext context) {
    final roleText = widget.profile.role == UserRole.admin ? 'ADMIN' : 'USER';

    return RefreshIndicator(
      onRefresh: () async {
        try {
          final updated = await widget.authRepository.fetchProfile();
          if (updated != null) {
            widget.onProfileUpdated(updated);
          }
        } catch (_) {
          // Keep current state.
        }
      },
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '내 계정',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 10),
                  _InfoRow(label: '역할', value: roleText),
                  _InfoRow(label: '사용자 ID', value: widget.profile.userId),
                  if (widget.profile.displayName != null)
                    _InfoRow(
                      label: '표시 이름',
                      value: widget.profile.displayName!,
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '개인 정보',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _churchController,
                    enabled: !_savingProfile,
                    decoration: const InputDecoration(
                      labelText: '교회',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _nameController,
                    enabled: !_savingProfile,
                    decoration: const InputDecoration(
                      labelText: '이름',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _groupController,
                    enabled: !_savingProfile,
                    decoration: const InputDecoration(
                      labelText: '구역',
                      border: OutlineInputBorder(),
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
                  const SizedBox(height: 12),
                  Align(
                    alignment: Alignment.centerRight,
                    child: FilledButton(
                      onPressed: _savingProfile ? null : _saveProfile,
                      child: Text(_savingProfile ? '저장 중...' : '프로필 저장'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '설정',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 10),
                  OutlinedButton.icon(
                    onPressed: _clearingCache ? null : _clearCache,
                    icon: _clearingCache
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.cleaning_services_outlined),
                    label: Text(_clearingCache ? '정리 중...' : '개인 캐시 정리'),
                  ),
                  const SizedBox(height: 8),
                  FilledButton.tonalIcon(
                    onPressed: _loggingOut ? null : _logout,
                    icon: _loggingOut
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.logout),
                    label: Text(_loggingOut ? '로그아웃 중...' : '로그아웃'),
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

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 76,
            child: Text(
              label,
              style: const TextStyle(
                color: Color(0xFF5B6572),
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}
