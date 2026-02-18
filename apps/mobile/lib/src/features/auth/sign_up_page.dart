import 'package:flutter/material.dart';

class SignUpPage extends StatefulWidget {
  const SignUpPage({super.key});

  @override
  State<SignUpPage> createState() => _SignUpPageState();
}

class _SignUpPageState extends State<SignUpPage> {
  final _loginIdController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  String? _error;
  bool _submitting = false;

  @override
  void dispose() {
    _loginIdController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    final loginId = _loginIdController.text.trim();
    final password = _passwordController.text;
    final confirm = _confirmPasswordController.text;

    if (loginId.isEmpty || password.isEmpty || confirm.isEmpty) {
      setState(() {
        _error = '아이디와 비밀번호를 입력해 주세요.';
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
      await Future.delayed(const Duration(milliseconds: 300));
      if (!mounted) {
        return;
      }
      await showDialog<void>(
        context: context,
        builder: (_) => AlertDialog(
          title: const Text('알림'),
          content: const Text('현재 회원가입은 준비 중입니다.'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('확인'),
            ),
          ],
        ),
      );
      if (!mounted) {
        return;
      }
      Navigator.of(context).pop();
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
                        '요청하신 계정 생성 API가 준비되면 바로 연결됩니다.',
                        style: TextStyle(color: Color(0xFF5B6572)),
                      ),
                      const SizedBox(height: 16),
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
