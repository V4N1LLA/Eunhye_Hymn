import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import 'auth_repository.dart';

class InviteCodePage extends StatefulWidget {
  final AuthRepository authRepository;
  final VoidCallback onBack;
  final VoidCallback onVerified;
  final Future<void> Function() onWithdrawn;

  const InviteCodePage({
    super.key,
    required this.authRepository,
    required this.onBack,
    required this.onVerified,
    required this.onWithdrawn,
  });

  @override
  State<InviteCodePage> createState() => _InviteCodePageState();
}

class _InviteCodePageState extends State<InviteCodePage> {
  final TextEditingController _controller = TextEditingController();
  bool _loading = false;
  String? _fieldError;

  bool get _canSubmit => _controller.text.trim().isNotEmpty && !_loading;

  @override
  void initState() {
    super.initState();
    _controller.addListener(() {
      setState(() {
        _fieldError = null;
      });
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _handleContinue() async {
    if (!_canSubmit) {
      return;
    }

    setState(() {
      _loading = true;
      _fieldError = null;
    });

    try {
      final valid =
          await widget.authRepository.verifyInviteCode(_controller.text);
      if (!mounted) {
        return;
      }
      if (!valid) {
        setState(() {
          _fieldError = '유효하지 않은 교회코드입니다.';
        });
        return;
      }
      widget.onVerified();
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _fieldError = e.message;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _fieldError = '교회코드 확인 중 오류가 발생했습니다.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  Future<void> _handleWithdraw() async {
    if (_loading) {
      return;
    }

    final shouldWithdraw = await showDialog<bool>(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('회원탈퇴'),
            content: const Text('정말 탈퇴하시겠어요? 탈퇴 후에는 다시 로그인해야 합니다.'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(false),
                child: const Text('취소'),
              ),
              FilledButton(
                onPressed: () => Navigator.of(context).pop(true),
                child: const Text('확인'),
              ),
            ],
          ),
        ) ??
        false;
    if (!shouldWithdraw) {
      return;
    }

    setState(() {
      _loading = true;
      _fieldError = null;
    });

    try {
      await widget.authRepository.withdrawAccount();
      await widget.authRepository.logout();
      await widget.onWithdrawn();
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.message)),
      );
    } catch (_) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('회원탈퇴 처리 중 오류가 발생했습니다.')),
      );
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
        leading: IconButton(
          onPressed: _loading ? null : widget.onBack,
          icon: const Icon(Icons.arrow_back),
        ),
        title: const Text('성도 인증하기'),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                '교회코드를 입력해주세요.',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _controller,
                enabled: !_loading,
                textInputAction: TextInputAction.done,
                decoration: InputDecoration(
                  labelText: '교회코드',
                  hintText: '교회코드',
                  errorText: _fieldError,
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                height: 52,
                child: FilledButton(
                  onPressed: _canSubmit ? _handleContinue : null,
                  child: Text(_loading ? '처리중...' : '계속하기'),
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                height: 52,
                child: OutlinedButton(
                  onPressed: _loading ? null : _handleWithdraw,
                  child: const Text('회원탈퇴'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
