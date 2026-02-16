import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../../core/config/app_config.dart';
import '../../core/network/api_exception.dart';
import 'auth_repository.dart';
import 'social_sdk_service.dart';

class LoginPage extends StatefulWidget {
  final AuthRepository authRepository;
  final SocialSdkService socialSdkService;
  final Future<void> Function(SessionProfile profile) onLoggedIn;

  const LoginPage({
    super.key,
    required this.authRepository,
    required this.socialSdkService,
    required this.onLoggedIn,
  });

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _inviteCodeController = TextEditingController();
  final _displayNameController = TextEditingController(text: '테스트 사용자');

  UserRole _devRole = UserRole.user;
  bool _loading = false;
  bool _showDevLogin = false;
  String? _error;

  @override
  void dispose() {
    _inviteCodeController.dispose();
    _displayNameController.dispose();
    super.dispose();
  }

  Future<void> _handleKakaoLogin() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final token = await widget.socialSdkService.fetchToken();
      final profile = await widget.authRepository.loginWithSocial(
        token: token,
        inviteCode: _inviteCodeController.text.trim().isEmpty
            ? null
            : _inviteCodeController.text.trim(),
      );
      await widget.onLoggedIn(profile);
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
        _error = '카카오 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  Future<void> _handleDevLogin() async {
    if (_displayNameController.text.trim().isEmpty) {
      setState(() {
        _error = '이름을 입력해 주세요.';
      });
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final profile = await widget.authRepository.loginWithDev(
        displayName: _displayNameController.text.trim(),
        userId: const Uuid().v4(),
        role: _devRole,
      );
      await widget.onLoggedIn(profile);
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
        _error = '개발용 로그인에 실패했습니다.';
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
    return Scaffold(
      backgroundColor: const Color(0xFFF6F7F9),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const _LoginHero(),
                  const SizedBox(height: 16),
                  Card(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(16, 20, 16, 16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const Text(
                            '카카오 로그인',
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(height: 6),
                          const Text(
                            '한 번만 로그인하면 자동으로 유지됩니다.',
                            style: TextStyle(color: Color(0xFF5B6572)),
                          ),
                          const SizedBox(height: 14),
                          TextField(
                            controller: _inviteCodeController,
                            enabled: !_loading,
                            decoration: InputDecoration(
                              labelText: '초대 코드 (최초 1회)',
                              hintText: '코드가 없다면 비워두세요',
                              filled: true,
                              fillColor: const Color(0xFFF9FAFB),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(12),
                                borderSide: BorderSide.none,
                              ),
                            ),
                          ),
                          const SizedBox(height: 14),
                          _SocialLoginButton(
                            onPressed: _loading ? null : _handleKakaoLogin,
                            label: _loading ? '로그인 중...' : '카카오로 시작하기',
                            icon: const Icon(Icons.chat_bubble_rounded),
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 10),
                    Container(
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFF1F0),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFFFFD1CC)),
                      ),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 10,
                      ),
                      child: Text(
                        _error!,
                        style: const TextStyle(
                          color: Color(0xFFC2291E),
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                  if (AppConfig.enableDevLogin) ...[
                    const SizedBox(height: 10),
                    OutlinedButton.icon(
                      onPressed: _loading
                          ? null
                          : () {
                              setState(() {
                                _showDevLogin = !_showDevLogin;
                              });
                            },
                      icon: Icon(
                        _showDevLogin ? Icons.expand_less : Icons.expand_more,
                      ),
                      label: const Text('개발용 로그인'),
                    ),
                    if (_showDevLogin)
                      _DevLoginPanel(
                        loading: _loading,
                        displayNameController: _displayNameController,
                        role: _devRole,
                        onRoleChanged: (nextRole) {
                          setState(() {
                            _devRole = nextRole;
                          });
                        },
                        onSubmit: _handleDevLogin,
                      ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _LoginHero extends StatelessWidget {
  const _LoginHero();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 72,
          height: 72,
          decoration: const BoxDecoration(
            color: Color(0xFFFF6F0F),
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.music_note_rounded,
              color: Colors.white, size: 36),
        ),
        const SizedBox(height: 12),
        const Text(
          '은혜찬송',
          style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 4),
        const Text(
          '모바일에서 빠르게 찬양 악보를 확인하세요.',
          style: TextStyle(color: Color(0xFF5B6572)),
        ),
      ],
    );
  }
}

class _SocialLoginButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final String label;
  final Widget icon;

  const _SocialLoginButton({
    required this.onPressed,
    required this.label,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 54,
      child: FilledButton.icon(
        onPressed: onPressed,
        icon: icon,
        label: Text(
          label,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
        ),
        style: FilledButton.styleFrom(
          backgroundColor: const Color(0xFFFEE500),
          foregroundColor: Colors.black87,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }
}

class _DevLoginPanel extends StatelessWidget {
  final bool loading;
  final TextEditingController displayNameController;
  final UserRole role;
  final ValueChanged<UserRole> onRoleChanged;
  final Future<void> Function() onSubmit;

  const _DevLoginPanel({
    required this.loading,
    required this.displayNameController,
    required this.role,
    required this.onRoleChanged,
    required this.onSubmit,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(top: 10),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: displayNameController,
              enabled: !loading,
              decoration: const InputDecoration(
                labelText: '이름',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 10),
            DropdownButtonFormField<UserRole>(
              initialValue: role,
              decoration: const InputDecoration(
                labelText: '권한',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: UserRole.user, child: Text('일반 사용자')),
                DropdownMenuItem(value: UserRole.admin, child: Text('관리자')),
              ],
              onChanged: loading
                  ? null
                  : (value) {
                      if (value != null) {
                        onRoleChanged(value);
                      }
                    },
            ),
            const SizedBox(height: 10),
            FilledButton.tonal(
              onPressed: loading ? null : () => onSubmit(),
              child: Text(loading ? '처리 중...' : '개발용 로그인'),
            ),
          ],
        ),
      ),
    );
  }
}
