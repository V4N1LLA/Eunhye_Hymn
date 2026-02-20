import 'package:flutter/material.dart';

import '../../core/config/app_config.dart';
import '../../core/network/api_exception.dart';
import 'auth_repository.dart';
import 'email_login_page.dart';
import 'invite_gate_page.dart';
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
  bool _loading = false;
  String? _error;

  bool get _hasKakaoKey => AppConfig.kakaoNativeAppKey.trim().isNotEmpty;

  bool _requiresInviteCode(ApiException exception) {
    return (exception.code ?? '').toLowerCase() == 'invalid_invite_code';
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

  Future<void> _handleWithdrawRequested() async {
    await widget.authRepository.withdraw();
  }

  Future<void> _openEmailLoginPage() async {
    if (_loading) {
      return;
    }

    final profile = await Navigator.of(context).push<SessionProfile>(
      MaterialPageRoute<SessionProfile>(
        builder: (_) => EmailLoginPage(
          authRepository: widget.authRepository,
        ),
      ),
    );

    if (!mounted || profile == null) {
      return;
    }

    await _runWithLoading(() async {
      await widget.onLoggedIn(profile);
    });
  }

  Future<void> _handleKakaoLogin() async {
    await _runWithLoading(() async {
      if (!_hasKakaoKey) {
        throw ApiException(
          'KAKAO_NATIVE_APP_KEY가 비어 있습니다. 앱 실행 시 '
          '--dart-define=KAKAO_NATIVE_APP_KEY=... 로 키를 전달해야 합니다.',
        );
      }

      final token = await widget.socialSdkService.fetchToken().timeout(
            const Duration(seconds: 45),
            onTimeout: () => throw ApiException(
              '카카오 로그인 응답이 지연되고 있습니다. 다시 시도해 주세요.',
            ),
          );

      try {
        final profile = await widget.authRepository.loginWithSocial(
          token: token,
        );
        await widget.onLoggedIn(profile);
      } on ApiException catch (e) {
        if (!_requiresInviteCode(e)) {
          rethrow;
        }

        if (!mounted) {
          return;
        }

        final profile = await Navigator.of(context).push<SessionProfile>(
          MaterialPageRoute<SessionProfile>(
            builder: (_) => InviteGatePage(
              title: '초대 코드 입력',
              description: '로그인을 완료하려면 초대 코드를 입력해 주세요.',
              submitLabel: '코드 확인 후 입장',
              onSubmit: (inviteCode) {
                return widget.authRepository.loginWithSocial(
                  token: token,
                  inviteCode: inviteCode,
                );
              },
              onWithdraw: _handleWithdrawRequested,
            ),
          ),
        );

        if (!mounted || profile == null) {
          return;
        }

        await widget.onLoggedIn(profile);
      }
    });
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
                            '카카오 또는 이메일 로그인을 선택해 주세요.',
                            style: TextStyle(color: Color(0xFF5B6572)),
                          ),
                          const SizedBox(height: 14),
                          _ActionButton(
                            onPressed: _loading ? null : _handleKakaoLogin,
                            label: _loading ? '처리 중...' : '카카오로 시작하기',
                            icon: const Icon(Icons.chat_bubble_rounded),
                            background: const Color(0xFFFEE500),
                            foreground: Colors.black87,
                          ),
                          const SizedBox(height: 10),
                          _ActionButton(
                            onPressed: _loading ? null : _openEmailLoginPage,
                            label: '이메일 로그인',
                            icon: const Icon(Icons.mail_outline),
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
          '은혜찬양',
          style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 4),
        const Text(
          '카카오 또는 이메일로 시작하세요.',
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
