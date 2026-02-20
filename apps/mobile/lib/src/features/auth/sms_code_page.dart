import 'dart:async';

import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import 'auth_repository.dart';

class SmsCodePage extends StatefulWidget {
  final AuthRepository authRepository;
  final VoidCallback onBack;
  final Future<void> Function() onCompleted;
  final String initialVerificationId;
  final String phoneNumber;
  final int initialCooldownSeconds;

  const SmsCodePage({
    super.key,
    required this.authRepository,
    required this.onBack,
    required this.onCompleted,
    required this.initialVerificationId,
    required this.phoneNumber,
    required this.initialCooldownSeconds,
  });

  @override
  State<SmsCodePage> createState() => _SmsCodePageState();
}

class _SmsCodePageState extends State<SmsCodePage> {
  final TextEditingController _controller = TextEditingController();

  late String _verificationId;
  late int _cooldownSeconds;
  Timer? _cooldownTimer;

  bool _loading = false;
  bool _verified = false;
  String? _fieldError;

  @override
  void initState() {
    super.initState();
    _verificationId = widget.initialVerificationId;
    _cooldownSeconds = widget.initialCooldownSeconds;
    _controller.addListener(() {
      if (mounted) {
        setState(() {
          _fieldError = null;
        });
      }
    });
    _startCooldownTimerIfNeeded();
  }

  @override
  void dispose() {
    _cooldownTimer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  String get _codeDigits => _controller.text.replaceAll(RegExp(r'\D'), '');

  bool get _canConfirm => _codeDigits.length == 6 && !_loading;

  void _startCooldownTimerIfNeeded() {
    _cooldownTimer?.cancel();
    if (_cooldownSeconds <= 0) {
      return;
    }
    _cooldownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      if (_cooldownSeconds <= 1) {
        setState(() {
          _cooldownSeconds = 0;
        });
        timer.cancel();
        return;
      }
      setState(() {
        _cooldownSeconds -= 1;
      });
    });
  }

  Future<void> _handleVerifyCode() async {
    if (!_canConfirm) {
      return;
    }

    setState(() {
      _loading = true;
      _fieldError = null;
    });

    try {
      final result = await widget.authRepository.verifySmsCode(
        _verificationId,
        _codeDigits,
      );
      if (!mounted) {
        return;
      }
      if (result.verified) {
        setState(() {
          _verified = true;
          _fieldError = null;
        });
      } else {
        setState(() {
          _verified = false;
          _fieldError = '인증코드가 올바르지 않습니다.';
        });
      }
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _verified = false;
        _fieldError = e.message;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _verified = false;
        _fieldError = '인증코드 확인 중 오류가 발생했습니다.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  Future<void> _handleResend() async {
    if (_loading || _cooldownSeconds > 0) {
      return;
    }

    setState(() {
      _loading = true;
      _fieldError = null;
    });

    try {
      final result =
          await widget.authRepository.requestSmsCode(widget.phoneNumber);
      if (!mounted) {
        return;
      }
      setState(() {
        _verificationId = result.verificationId;
        _cooldownSeconds = result.cooldownSeconds;
        _verified = false;
        _controller.clear();
      });
      _startCooldownTimerIfNeeded();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('인증번호를 다시 발송했습니다.')),
      );
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
        const SnackBar(content: Text('재전송에 실패했습니다.')),
      );
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  Future<void> _handleComplete() async {
    if (_loading || !_verified) {
      return;
    }

    setState(() {
      _loading = true;
    });

    try {
      await widget.onCompleted();
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
    final resendLabel = _cooldownSeconds > 0
        ? '문자를 받지 못하셨나요? 문자 재전송하기 (${_cooldownSeconds}s)'
        : '문자를 받지 못하셨나요? 문자 재전송하기';

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
                '문자로 받은 인증코드를 입력해주세요.',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      enabled: !_loading,
                      keyboardType: TextInputType.number,
                      maxLength: 6,
                      decoration: InputDecoration(
                        labelText: '인증코드',
                        hintText: '인증코드',
                        counterText: '',
                        errorText: _fieldError,
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  SizedBox(
                    height: 48,
                    child: FilledButton(
                      onPressed: _canConfirm ? _handleVerifyCode : null,
                      child: const Text('확인'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton(
                  onPressed:
                      (_loading || _cooldownSeconds > 0) ? null : _handleResend,
                  child: Text(resendLabel),
                ),
              ),
              const Spacer(),
              SizedBox(
                height: 52,
                child: FilledButton(
                  onPressed: (_verified && !_loading) ? _handleComplete : null,
                  child: Text(_loading ? '처리중...' : '성도 인증하기'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
