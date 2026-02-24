import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import 'auth_repository.dart';

typedef InviteCodeSubmit = Future<SessionProfile> Function(String inviteCode);

class InviteGatePage extends StatefulWidget {
  final String title;
  final String description;
  final String submitLabel;
  final InviteCodeSubmit onSubmit;
  final Future<void> Function()? onWithdraw;

  const InviteGatePage({
    super.key,
    required this.title,
    required this.description,
    required this.submitLabel,
    required this.onSubmit,
    this.onWithdraw,
  });

  @override
  State<InviteGatePage> createState() => _InviteGatePageState();
}

class _InviteGatePageState extends State<InviteGatePage> {
  final _inviteCodeController = TextEditingController();

  bool _submitting = false;
  String? _error;

  @override
  void dispose() {
    _inviteCodeController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    final inviteCode = _inviteCodeController.text.trim().toUpperCase();
    if (inviteCode.isEmpty) {
      setState(() {
        _error = '초대 코드를 입력해 주세요.';
      });
      return;
    }

    setState(() {
      _submitting = true;
      _error = null;
    });

    try {
      final profile = await widget.onSubmit(inviteCode);
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
        _error = '초대 코드 확인 중 문제가 발생했습니다. 다시 시도해 주세요.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _submitting = false;
        });
      }
    }
  }

  Future<void> _handleWithdraw() async {
    if (widget.onWithdraw == null || _submitting) {
      return;
    }

    final confirmed = await showDialog<bool>(
          context: context,
          builder: (_) => AlertDialog(
            title: const Text('회원탈퇴'),
            content: const Text('정말 회원탈퇴 하시겠어요?'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(false),
                child: const Text('취소'),
              ),
              FilledButton(
                onPressed: () => Navigator.of(context).pop(true),
                child: const Text('탈퇴'),
              ),
            ],
          ),
        ) ??
        false;

    if (!confirmed) {
      return;
    }

    setState(() {
      _submitting = true;
      _error = null;
    });

    try {
      await widget.onWithdraw!.call();
      if (!mounted) {
        return;
      }
      Navigator.of(context).pop();
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
        _error = '회원탈퇴 처리 중 문제가 발생했습니다. 다시 시도해 주세요.';
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
      backgroundColor: const Color(0xFFF6F7F9),
      appBar: AppBar(title: Text(widget.title)),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        widget.title,
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        widget.description,
                        style: const TextStyle(color: Color(0xFF5B6572)),
                      ),
                      const SizedBox(height: 14),
                      TextField(
                        controller: _inviteCodeController,
                        enabled: !_submitting,
                        textCapitalization: TextCapitalization.characters,
                        decoration: const InputDecoration(
                          labelText: '초대 코드',
                          hintText: '예: ABC123',
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
                          child: Text(
                              _submitting ? '처리 중...' : widget.submitLabel),
                        ),
                      ),
                      if (widget.onWithdraw != null) ...[
                        const SizedBox(height: 8),
                        Center(
                          child: TextButton(
                            onPressed: _submitting ? null : _handleWithdraw,
                            style: TextButton.styleFrom(
                              foregroundColor: const Color(0xFF6B7280),
                              textStyle: const TextStyle(fontSize: 12),
                            ),
                            child: const Text('회원탈퇴'),
                          ),
                        ),
                      ],
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
