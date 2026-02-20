import 'package:flutter/material.dart';

import '../../core/config/app_config.dart';
import '../../core/network/api_exception.dart';
import 'auth_repository.dart';
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
    required this.onLoggedIn,
    this.initialError,
  });

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  bool _socialLoading = false;
  bool _emailLoading = false;
  String? _error;

  bool get _loading => _socialLoading || _emailLoading;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleKakaoLogin() async {
    if (_loading) {
      return;
    }

    setState(() {
      _socialLoading = true;
      _error = null;
    });

    try {
      if (AppConfig.kakaoNativeAppKey.trim().isEmpty) {
        throw ApiException(
          '카카오 로그인 설정이 비어 있어요. 잠시 후 다시 시도해 주세요.',
        );
      }

      final token = await widget.socialSdkService.fetchToken().timeout(
            const Duration(seconds: 45),
            onTimeout: () => throw ApiException('로그인 시간이 초과됐어요. 다시 시도해 주세요.'),
          );
      final profile = await widget.authRepository.loginWithKakao(token: token);
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
        _error = '로그인에 실패했어요. 다시 시도해 주세요.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _socialLoading = false;
        });
      }
    }
  }

  Future<void> _handleEmailLogin() async {
    if (_loading) {
      return;
    }

    final loginId = _emailController.text.trim();
    final password = _passwordController.text;
    if (loginId.isEmpty || password.isEmpty) {
      setState(() {
        _error = '이메일(또는 아이디)과 비밀번호를 입력해 주세요.';
      });
      return;
    }

    setState(() {
      _emailLoading = true;
      _error = null;
    });

    try {
      final profile = await widget.authRepository.loginWithAccount(
        loginId: loginId,
        password: password,
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
        _error = '로그인에 실패했어요. 다시 시도해 주세요.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _emailLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final visibleError = _error ?? widget.initialError;

    return Scaffold(
      backgroundColor: const Color(0xFFF6F7F9),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
          child: Column(
            children: [
              const Spacer(),
              const _LoginHero(),
              const SizedBox(height: 12),
              const Text(
                '은혜를 더 가까이, 찬양을 더 편하게',
                style: TextStyle(
                  color: Color(0xFF5B6572),
                  fontSize: 14,
                ),
              ),
              const Spacer(),
              if (visibleError != null) ...[
                Container(
                  width: double.infinity,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF1F0),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: const Color(0xFFFFD1CC)),
                  ),
                  child: Text(
                    visibleError,
                    style: const TextStyle(
                      color: Color(0xFFC2291E),
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const SizedBox(height: 12),
              ],
              SizedBox(
                width: double.infinity,
                height: 56,
                child: FilledButton(
                  onPressed: _loading ? null : _handleKakaoLogin,
                  style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFFFEE500),
                    foregroundColor: Colors.black87,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        width: 24,
                        height: 24,
                        decoration: const BoxDecoration(
                          color: Colors.black87,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.chat_bubble_rounded,
                          color: Color(0xFFFEE500),
                          size: 14,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Text(
                        _socialLoading ? '카카오 로그인 중...' : '카카오로 시작하기',
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 14),
              const Row(
                children: [
                  Expanded(child: Divider()),
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 10),
                    child: Text(
                      '또는',
                      style: TextStyle(color: Color(0xFF6B7280)),
                    ),
                  ),
                  Expanded(child: Divider()),
                ],
              ),
              const SizedBox(height: 14),
              TextField(
                controller: _emailController,
                enabled: !_loading,
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
                decoration: const InputDecoration(
                  labelText: '이메일(또는 아이디)',
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _passwordController,
                enabled: !_loading,
                obscureText: true,
                onSubmitted: (_) => _handleEmailLogin(),
                decoration: const InputDecoration(
                  labelText: '비밀번호',
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _loading ? null : _handleEmailLogin,
                  icon: const Icon(Icons.mail_outline),
                  label: Text(_emailLoading ? '로그인 중...' : '이메일로 로그인'),
                ),
              ),
            ],
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
          width: 84,
          height: 84,
          decoration: const BoxDecoration(
            color: Color(0xFFFF6F0F),
            shape: BoxShape.circle,
          ),
          child: const Icon(
            Icons.music_note_rounded,
            color: Colors.white,
            size: 42,
          ),
        ),
        const SizedBox(height: 14),
        const Text(
          '은혜찬송',
          style: TextStyle(
            fontSize: 30,
            fontWeight: FontWeight.w800,
          ),
        ),
      ],
    );
  }
}
