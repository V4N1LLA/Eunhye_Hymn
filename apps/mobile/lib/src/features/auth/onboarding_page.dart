import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import '../../core/storage/onboarding_storage.dart';
import 'auth_repository.dart';

class OnboardingPage extends StatefulWidget {
  final String userId;
  final AuthRepository authRepository;
  final OnboardingStorage onboardingStorage;
  final Future<void> Function(SessionProfile profile) onCompleted;

  const OnboardingPage({
    super.key,
    required this.userId,
    required this.authRepository,
    required this.onboardingStorage,
    required this.onCompleted,
  });

  @override
  State<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<OnboardingPage> {
  final _churchController = TextEditingController();
  final _nameController = TextEditingController();
  final _groupController = TextEditingController();

  UserGender _gender = UserGender.unknown;
  String? _error;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  @override
  void dispose() {
    _churchController.dispose();
    _nameController.dispose();
    _groupController.dispose();
    super.dispose();
  }

  Future<void> _loadProfile() async {
    final profile = await widget.onboardingStorage.getProfile(widget.userId);
    if (!mounted || profile == null) {
      return;
    }

    setState(() {
      _churchController.text = profile.churchName;
      _nameController.text = profile.name;
      _groupController.text = profile.group;
      _gender = _parseGender(profile.gender);
    });
  }

  Future<void> _handleSubmit() async {
    final churchName = _churchController.text.trim();
    final name = _nameController.text.trim();
    final group = _groupController.text.trim();

    if (churchName.isEmpty || name.isEmpty || group.isEmpty) {
      setState(() {
        _error = '모든 항목을 입력해 주세요.';
      });
      return;
    }

    if (_gender == UserGender.unknown) {
      setState(() {
        _error = '성별을 선택해 주세요.';
      });
      return;
    }

    setState(() {
      _saving = true;
      _error = null;
    });

    try {
      final updatedProfile = await widget.authRepository.updateProfile(
        churchName: churchName,
        name: name,
        group: group,
        gender: _gender,
      );
      await widget.onboardingStorage.saveProfile(
        userId: widget.userId,
        churchName: churchName,
        name: name,
        group: group,
        gender: _genderApiValue(_gender),
      );
      await widget.onCompleted(updatedProfile);
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
        _error = '정보 저장 중 문제가 발생했습니다. 다시 시도해 주세요.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _saving = false;
        });
      }
    }
  }

  UserGender _parseGender(String raw) {
    return switch (raw.trim().toUpperCase()) {
      'MALE' => UserGender.male,
      'FEMALE' => UserGender.female,
      _ => UserGender.unknown,
    };
  }

  String _genderApiValue(UserGender gender) {
    return switch (gender) {
      UserGender.male => 'MALE',
      UserGender.female => 'FEMALE',
      UserGender.unknown => 'UNKNOWN',
    };
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('회원 정보 입력')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
          children: [
            const Text(
              '기본 정보를 입력해 주세요.',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 6),
            const Text(
              '입력한 정보는 관리자 승인 및 안내 표시에 사용됩니다.',
              style: TextStyle(color: Color(0xFF6B7280)),
            ),
            const SizedBox(height: 18),
            TextField(
              controller: _churchController,
              enabled: !_saving,
              decoration: const InputDecoration(
                labelText: '교회',
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _nameController,
              enabled: !_saving,
              decoration: const InputDecoration(
                labelText: '이름',
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _groupController,
              enabled: !_saving,
              decoration: const InputDecoration(
                labelText: '구역',
              ),
            ),
            const SizedBox(height: 12),
            InputDecorator(
              decoration: const InputDecoration(
                labelText: '성별',
              ),
              child: SegmentedButton<UserGender>(
                showSelectedIcon: false,
                selected: {_gender},
                onSelectionChanged: _saving
                    ? null
                    : (selection) {
                        if (selection.isEmpty) {
                          return;
                        }
                        setState(() {
                          _gender = selection.first;
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
              const SizedBox(height: 12),
              Text(
                _error!,
                style: const TextStyle(
                  color: Color(0xFFC2291E),
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _saving ? null : _handleSubmit,
              child: Text(_saving ? '저장 중...' : '완료'),
            ),
          ],
        ),
      ),
    );
  }
}
