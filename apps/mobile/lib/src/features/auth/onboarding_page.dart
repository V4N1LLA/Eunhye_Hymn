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

    setState(() {
      _saving = true;
      _error = null;
    });

    try {
      final updatedProfile = await widget.authRepository.updateProfile(
        churchName: churchName,
        name: name,
        group: group,
      );
      await widget.onboardingStorage.saveProfile(
        userId: widget.userId,
        churchName: churchName,
        name: name,
        group: group,
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
        _error = '프로필 저장 중 문제가 발생했습니다. 다시 시도해 주세요.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _saving = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F7F9),
      appBar: AppBar(title: const Text('회원 정보 입력')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        '교회 / 구역 / 이름을 입력해 주세요.',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 6),
                      const Text(
                        '입력값은 계정 프로필로 저장되어 다른 기기에서도 동기화됩니다.',
                        style: TextStyle(color: Color(0xFF5B6572)),
                      ),
                      const SizedBox(height: 16),
                      TextField(
                        controller: _churchController,
                        enabled: !_saving,
                        decoration: const InputDecoration(
                          labelText: '교회',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _nameController,
                        enabled: !_saving,
                        decoration: const InputDecoration(
                          labelText: '이름',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _groupController,
                        enabled: !_saving,
                        decoration: const InputDecoration(
                          labelText: '구역',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 16),
                      SizedBox(
                        height: 52,
                        child: FilledButton(
                          onPressed: _saving ? null : _handleSubmit,
                          child: Text(_saving ? '처리 중...' : '완료'),
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
                      const Center(
                        child: Text(
                          '회원/교회 정보는 첫 사용시에만 입력합니다.',
                          style: TextStyle(
                            color: Color(0xFF6B7280),
                            fontSize: 12,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
