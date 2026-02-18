import 'package:flutter/material.dart';

import '../../core/config/app_config.dart';
import '../../core/network/api_exception.dart';
import 'auth_repository.dart';
import 'sign_up_page.dart';
import 'social_sdk_service.dart';

class LoginPage extends StatefulWidget {
  final AuthRepository authRepository;
  final SocialSdkService socialSdkService;
  final Future<void> Function(SessionProfile profile) onLoggedIn;
  final String? initialError;

  const LoginPage({
    super.key,
    required this.authRepository,
    required this.socialSdkService,
    this.initialError,
    required this.onLoggedIn,
  });

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _inviteCodeController = TextEditingController();
  final _accountIdController = TextEditingController();
  final _accountPasswordController = TextEditingController();

  bool _loading = false;
  bool _showAccountLogin = false;
  String? _error;

  bool get _hasKakaoKey => AppConfig.kakaoNativeAppKey.trim().isNotEmpty;

  String get _normalizedInviteCode =>
      _inviteCodeController.text.trim().toUpperCase();

  String? _validateInviteCode() {
    final inviteCode = _normalizedInviteCode;
    if (inviteCode.isEmpty) {
      return '초대 코드를 입력해 주세요.';
    }
    return null;
  }

  @override
  void dispose() {
    _inviteCodeController.dispose();
    _accountIdController.dispose();
    _accountPasswordController.dispose();
    super.dispose();
  }

  Future<void> _runWithLoading(Future<void> Function() action) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await action();
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
        _error = '작업 중 오류가 발생했습니다. 다시 시도해 주세요.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  Future<void> _handleKakaoLogin() async {
    final inviteCodeError = _validateInviteCode();
    if (inviteCodeError != null) {
      setState(() {
        _error = inviteCodeError;
      });
      return;
    }

    await _runWithLoading(() async {
      if (!_hasKakaoKey) {
        throw ApiException(
          'KAKAO_NATIVE_APP_KEY가 비어 있습니다. 앱 실행 시 '
          '--dart-define=KAKAO_NATIVE_APP_KEY=... 로 키를 전달해야 합니다.',
        );
      }

      final token = await widget.socialSdkService.fetchToken();
      final profile = await widget.authRepository.loginWithSocial(
        token: token,
        inviteCode: _normalizedInviteCode,
      );
      await widget.onLoggedIn(profile);
    });
  }

  Future<void> _handleAccountLogin() async {
    final inviteCodeError = _validateInviteCode();
    if (inviteCodeError != null) {
      setState(() {
        _error = inviteCodeError;
      });
      return;
    }

    final loginId = _accountIdController.text.trim();
    final password = _accountPasswordController.text;
    if (loginId.isEmpty || password.isEmpty) {
      setState(() {
        _error = '아이디와 비밀번호를 모두 입력해 주세요.';
      });
      return;
    }

    if (!_isValidUuid(loginId)) {
      setState(() {
        _error = '현재는 DB에 저장된 사용자 ID(UUID) 형식만 사용 가능합니다.';
      });
      return;
    }

    await _runWithLoading(() async {
      final profile = await widget.authRepository.loginWithDev(
        displayName: loginId,
        userId: loginId,
        role: UserRole.user,
      );
      await widget.onLoggedIn(profile);
    });
  }

  bool _isValidUuid(String value) {
    return RegExp(r'^[0-9a-fA-F-]{36}$').hasMatch(value);
  }

  Future<void> _openSignUp() async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => const SignUpPage(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final visibleError = _error ?? widget.initialError;
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
                            '로그인',
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(height: 6),
                          const Text(
                            '초대 코드를 입력한 뒤 로그인 방법을 선택해 주세요.',
                            style: TextStyle(color: Color(0xFF5B6572)),
                          ),
                          const SizedBox(height: 14),
                          TextField(
                            controller: _inviteCodeController,
                            enabled: !_loading,
                            decoration: const InputDecoration(
                              labelText: '초대 코드',
                              hintText: '예: ABC123',
                              filled: true,
                              fillColor: Color(0xFFF9FAFB),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.all(
                                  Radius.circular(12),
                                ),
                                borderSide: BorderSide.none,
                              ),
                            ),
                          ),
                          const SizedBox(height: 12),
                          _ActionButton(
                            onPressed: _loading ? null : _handleKakaoLogin,
                            label: _loading ? '처리 중...' : '카카오로 시작하기',
                            icon: const Icon(Icons.chat_bubble_rounded),
                            background: const Color(0xFFFEE500),
                            foreground: Colors.black87,
                          ),
                          const SizedBox(height: 10),
                          _ActionButton(
                            onPressed: _loading
                                ? null
                                : () => setState(() {
                                      _showAccountLogin = !_showAccountLogin;
                                    }),
                            label: _showAccountLogin
                                ? '계정 시작 닫기'
                                : '아이디/비밀번호로 시작하기',
                            icon: const Icon(Icons.lock_outline),
                            background: Colors.white,
                            foreground: const Color(0xFF111827),
                            border: const Color(0xFFE5E7EB),
                          ),
                          if (!_hasKakaoKey && !_loading)
                            const Padding(
                              padding: EdgeInsets.only(top: 10),
                              child: Text(
                                '카카오 앱키가 없어서 카카오 로그인은 비활성화됩니다.',
                                style:
                                    TextStyle(fontSize: 12, color: Colors.red),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
                  if (visibleError != null) ...[
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
                        visibleError,
                        style: const TextStyle(
                          color: Color(0xFFC2291E),
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                  if (_showAccountLogin) ...[
                    const SizedBox(height: 10),
                    Card(
                      margin: EdgeInsets.zero,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            TextField(
                              controller: _accountIdController,
                              enabled: !_loading,
                              decoration: const InputDecoration(
                                labelText: '아이디',
                                border: OutlineInputBorder(),
                              ),
                            ),
                            const SizedBox(height: 10),
                            TextField(
                              controller: _accountPasswordController,
                              enabled: !_loading,
                              obscureText: true,
                              decoration: const InputDecoration(
                                labelText: '비밀번호',
                                border: OutlineInputBorder(),
                              ),
                            ),
                            const SizedBox(height: 12),
                            SizedBox(
                              height: 48,
                              child: FilledButton(
                                onPressed:
                                    _loading ? null : _handleAccountLogin,
                                child: Text(
                                  _loading ? '처리 중...' : '로그인',
                                ),
                              ),
                            ),
                            const SizedBox(height: 8),
                            SizedBox(
                              height: 48,
                              child: OutlinedButton(
                                onPressed: _loading ? null : _openSignUp,
                                child: const Text('회원가입'),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 30),
                  const Center(
                    child: Text(
                      '회원/교회 정보는 첫 사용 시에만 입력합니다.',
                      style: TextStyle(color: Color(0xFF6B7280), fontSize: 12),
                    ),
                  ),
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
          '초대 코드를 입력하고 바로 시작하세요.',
          style: TextStyle(color: Color(0xFF5B6572)),
        ),
      ],
    );
  }
}

class _ActionButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final String label;
  final Widget icon;
  final Color background;
  final Color foreground;
  final Color border;

  const _ActionButton({
    required this.onPressed,
    required this.label,
    required this.icon,
    required this.background,
    required this.foreground,
    this.border = Colors.transparent,
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
          backgroundColor: background,
          foregroundColor: foreground,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          side: border == Colors.transparent ? null : BorderSide(color: border),
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
    );
  }
}
