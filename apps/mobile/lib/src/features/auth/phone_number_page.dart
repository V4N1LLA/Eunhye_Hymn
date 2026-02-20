import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import 'auth_repository.dart';

class PhoneNumberPage extends StatefulWidget {
  final AuthRepository authRepository;
  final VoidCallback onBack;
  final void Function(String phoneNumber, SmsRequestResult result) onRequested;

  const PhoneNumberPage({
    super.key,
    required this.authRepository,
    required this.onBack,
    required this.onRequested,
  });

  @override
  State<PhoneNumberPage> createState() => _PhoneNumberPageState();
}

class _PhoneNumberPageState extends State<PhoneNumberPage> {
  final TextEditingController _controller = TextEditingController();
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _controller.addListener(() {
      if (mounted) {
        setState(() {});
      }
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  String _digitsOnly(String input) {
    return input.replaceAll(RegExp(r'\D'), '');
  }

  bool get _isValidPhone {
    final digits = _digitsOnly(_controller.text);
    return digits.length >= 10 && digits.length <= 11;
  }

  Future<void> _handleSend() async {
    if (_loading || !_isValidPhone) {
      return;
    }

    final phoneNumber = _digitsOnly(_controller.text);
    setState(() {
      _loading = true;
    });

    try {
      final result = await widget.authRepository.requestSmsCode(phoneNumber);
      if (!mounted) {
        return;
      }
      widget.onRequested(phoneNumber, result);
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
        const SnackBar(content: Text('인증번호 발송에 실패했습니다.')),
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
                '휴대폰 번호를 입력해주세요.',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _controller,
                enabled: !_loading,
                keyboardType: TextInputType.phone,
                decoration: const InputDecoration(
                  labelText: '휴대폰 번호',
                  hintText: '휴대폰 번호',
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                height: 52,
                child: FilledButton(
                  onPressed: _isValidPhone && !_loading ? _handleSend : null,
                  child: Text(_loading ? '처리중...' : '발송'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
