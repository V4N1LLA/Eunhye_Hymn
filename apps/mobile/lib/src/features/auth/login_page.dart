import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../../core/network/api_exception.dart';
import 'auth_repository.dart';

class LoginPage extends StatefulWidget {
  final AuthRepository authRepository;
  final void Function(SessionProfile profile) onLoggedIn;

  const LoginPage({
    super.key,
    required this.authRepository,
    required this.onLoggedIn,
  });

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _socialTokenController = TextEditingController();
  final _inviteCodeController = TextEditingController();
  final _displayNameController = TextEditingController(text: '모바일관리자');

  SocialProvider _provider = SocialProvider.google;
  UserRole _devRole = UserRole.user;
  bool _loading = false;
  bool _showDevLogin = false;
  String? _error;

  @override
  void dispose() {
    _socialTokenController.dispose();
    _inviteCodeController.dispose();
    _displayNameController.dispose();
    super.dispose();
  }

  Future<void> _handleSocialLogin() async {
    if (_socialTokenController.text.trim().isEmpty) {
      setState(() {
        _error = '소셜 토큰을 입력하세요.';
      });
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final profile = await widget.authRepository.loginWithSocial(
        provider: _provider,
        token: _socialTokenController.text.trim(),
        inviteCode: _inviteCodeController.text.trim().isEmpty
            ? null
            : _inviteCodeController.text.trim(),
      );
      widget.onLoggedIn(profile);
    } on ApiException catch (e) {
      setState(() {
        _error = e.message;
      });
    } catch (_) {
      setState(() {
        _error = '로그인 중 알 수 없는 오류가 발생했습니다.';
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
        _error = '표시 이름을 입력하세요.';
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
      widget.onLoggedIn(profile);
    } on ApiException catch (e) {
      setState(() {
        _error = e.message;
      });
    } catch (_) {
      setState(() {
        _error = 'Dev 로그인 중 알 수 없는 오류가 발생했습니다.';
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
      appBar: AppBar(
        title: const Text('Eunhye Hymn 로그인'),
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Text(
                        _error!,
                        style: const TextStyle(color: Colors.red),
                      ),
                    ),
                  const Text(
                    '소셜 로그인',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<SocialProvider>(
                    value: _provider,
                    decoration: const InputDecoration(
                      labelText: 'Provider',
                      border: OutlineInputBorder(),
                    ),
                    items: const [
                      DropdownMenuItem(
                        value: SocialProvider.google,
                        child: Text('Google'),
                      ),
                      DropdownMenuItem(
                        value: SocialProvider.kakao,
                        child: Text('Kakao'),
                      ),
                    ],
                    onChanged: _loading
                        ? null
                        : (value) {
                            if (value != null) {
                              setState(() => _provider = value);
                            }
                          },
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _socialTokenController,
                    enabled: !_loading,
                    decoration: const InputDecoration(
                      labelText: 'ID Token',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _inviteCodeController,
                    enabled: !_loading,
                    decoration: const InputDecoration(
                      labelText: '초대코드 (신규 사용자만)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: _loading ? null : _handleSocialLogin,
                    child: Text(_loading ? '로그인 중...' : '소셜 로그인'),
                  ),
                  const SizedBox(height: 20),
                  OutlinedButton.icon(
                    onPressed: _loading
                        ? null
                        : () {
                            setState(() => _showDevLogin = !_showDevLogin);
                          },
                    icon: Icon(_showDevLogin ? Icons.expand_less : Icons.expand_more),
                    label: const Text('Dev 로그인 (개발용)'),
                  ),
                  if (_showDevLogin) ...[
                    const SizedBox(height: 12),
                    TextField(
                      controller: _displayNameController,
                      enabled: !_loading,
                      decoration: const InputDecoration(
                        labelText: '표시 이름',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<UserRole>(
                      value: _devRole,
                      decoration: const InputDecoration(
                        labelText: '역할',
                        border: OutlineInputBorder(),
                      ),
                      items: const [
                        DropdownMenuItem(
                          value: UserRole.user,
                          child: Text('USER'),
                        ),
                        DropdownMenuItem(
                          value: UserRole.admin,
                          child: Text('ADMIN'),
                        ),
                      ],
                      onChanged: _loading
                          ? null
                          : (value) {
                              if (value != null) {
                                setState(() => _devRole = value);
                              }
                            },
                    ),
                    const SizedBox(height: 12),
                    FilledButton.tonal(
                      onPressed: _loading ? null : _handleDevLogin,
                      child: Text(_loading ? '처리 중...' : 'Dev 로그인'),
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
