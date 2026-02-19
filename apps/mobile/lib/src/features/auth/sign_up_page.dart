import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import 'auth_repository.dart';

class SignUpPage extends StatefulWidget {
  final AuthRepository authRepository;
  final String initialInviteCode;

  const SignUpPage({
    super.key,
    required this.authRepository,
    this.initialInviteCode = '',
  });

  @override
  State<SignUpPage> createState() => _SignUpPageState();
}

class _SignUpPageState extends State<SignUpPage> {
  late final TextEditingController _inviteCodeController;
  final _loginIdController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  String? _error;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _inviteCodeController = TextEditingController(
      text: widget.initialInviteCode.trim(),
    );
  }

  @override
  void dispose() {
    _inviteCodeController.dispose();
    _loginIdController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    final inviteCode = _inviteCodeController.text.trim().toUpperCase();
    final loginId = _loginIdController.text.trim();
    final password = _passwordController.text;
    final confirm = _confirmPasswordController.text;

    if (inviteCode.isEmpty || loginId.isEmpty || password.isEmpty || confirm.isEmpty) {
      setState(() {
        _error = '초대코드, 아이디, 비밀번호를 모두 입력해 주세요.';
      });
      return;
    }

    if (password.length < 8) {
      setState(() {
        _error = '비밀번호는 최소 8자 이상이어야 합니다.';
      });
      return;
    }

    if (password != confirm) {
      setState(() {
        _error = '비밀번호가 일치하지 않습니다.';
      });
      return;
    }

    setState(() {
      _submitting = true;
      _error = null;
    });

    try {
      final profile = await widget.authRepository.signupWithAccount(
        loginId: loginId,
        password: password,
        inviteCode: inviteCode,
      );
      if (!mounted) {
        return;
      }
      Navigator.of(context).pop(profile);
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
        _error = '회원가입 처리 중 오류가 발생했습니다. 다시 시도해 주세요.';
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
      appBar: AppBar(title: const Text('회원가입')),
      backgroundColor: const Color(0xFFF6F7F9),
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
                        '회원가입',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 6),
                      const Text(
                        '초대코드가 확인되면 계정이 생성되고 바로 로그인됩니다.',
                        style: TextStyle(color: Color(0xFF5B6572)),
                      ),
                      const SizedBox(height: 16),
                      TextField(
                        controller: _inviteCodeController,
                        enabled: !_submitting,
                        decoration: const InputDecoration(
                          labelText: '초대 코드',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _loginIdController,
                        enabled: !_submitting,
                        decoration: const InputDecoration(
                          labelText: '아이디',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _passwordController,
                        enabled: !_submitting,
                        obscureText: true,
                        decoration: const InputDecoration(
                          labelText: '비밀번호',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _confirmPasswordController,
                        enabled: !_submitting,
                        obscureText: true,
                        decoration: const InputDecoration(
                          labelText: '비밀번호 확인',
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
                      const SizedBox(height: 16),
                      SizedBox(
                        height: 52,
                        child: FilledButton(
                          onPressed: _submitting ? null : _handleSubmit,
                          child:
                              Text(_submitting ? '처리 중...' : '회원가입 하기'),
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
