import 'package:flutter/material.dart';

import 'auth_repository.dart';
import 'invite_code_page.dart';
import 'phone_number_page.dart';
import 'sms_code_page.dart';

enum _VerificationStep {
  invite,
  phone,
  sms,
}

class AuthVerificationFlow extends StatefulWidget {
  final AuthRepository authRepository;
  final SessionProfile profile;
  final Future<void> Function(SessionProfile profile) onVerified;
  final Future<void> Function() onReturnToLogin;

  const AuthVerificationFlow({
    super.key,
    required this.authRepository,
    required this.profile,
    required this.onVerified,
    required this.onReturnToLogin,
  });

  @override
  State<AuthVerificationFlow> createState() => _AuthVerificationFlowState();
}

class _AuthVerificationFlowState extends State<AuthVerificationFlow> {
  late SessionProfile _profile;
  late _VerificationStep _step;

  String? _phoneNumber;
  SmsRequestResult? _smsRequestResult;
  bool _processing = false;

  @override
  void initState() {
    super.initState();
    _profile = widget.profile;
    _step = _initialStep(_profile);

    if (_profile.verified) {
      WidgetsBinding.instance.addPostFrameCallback((_) async {
        await widget.onVerified(_profile);
      });
    }
  }

  _VerificationStep _initialStep(SessionProfile profile) {
    if (!profile.inviteVerified) {
      return _VerificationStep.invite;
    }
    if (!profile.phoneVerified) {
      return _VerificationStep.phone;
    }
    return _VerificationStep.invite;
  }

  Future<void> _backToLoginWithLogout() async {
    if (_processing) {
      return;
    }
    setState(() {
      _processing = true;
    });
    try {
      await widget.authRepository.logout();
      await widget.onReturnToLogin();
    } finally {
      if (mounted) {
        setState(() {
          _processing = false;
        });
      }
    }
  }

  void _goToPhoneStep() {
    setState(() {
      _profile = _profile.copyWith(inviteVerified: true);
      _step = _VerificationStep.phone;
      _smsRequestResult = null;
      _phoneNumber = null;
    });
  }

  void _goToSmsStep(String phoneNumber, SmsRequestResult result) {
    setState(() {
      _phoneNumber = phoneNumber;
      _smsRequestResult = result;
      _step = _VerificationStep.sms;
    });
  }

  Future<void> _completeVerification() async {
    if (_processing) {
      return;
    }
    setState(() {
      _processing = true;
    });

    try {
      final refreshed = await widget.authRepository.fetchProfile();
      final profile = refreshed ??
          _profile.copyWith(
            inviteVerified: true,
            phoneVerified: true,
          );
      await widget.onVerified(profile);
    } finally {
      if (mounted) {
        setState(() {
          _processing = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    switch (_step) {
      case _VerificationStep.invite:
        return InviteCodePage(
          authRepository: widget.authRepository,
          onBack: _processing ? () {} : () => _backToLoginWithLogout(),
          onVerified: _goToPhoneStep,
          onWithdrawn: widget.onReturnToLogin,
        );
      case _VerificationStep.phone:
        return PhoneNumberPage(
          authRepository: widget.authRepository,
          onBack: () {
            setState(() {
              _step = _VerificationStep.invite;
            });
          },
          onRequested: _goToSmsStep,
        );
      case _VerificationStep.sms:
        final request = _smsRequestResult;
        final phoneNumber = _phoneNumber;
        if (request == null || phoneNumber == null) {
          return PhoneNumberPage(
            authRepository: widget.authRepository,
            onBack: () {
              setState(() {
                _step = _VerificationStep.invite;
              });
            },
            onRequested: _goToSmsStep,
          );
        }
        return SmsCodePage(
          authRepository: widget.authRepository,
          initialVerificationId: request.verificationId,
          initialCooldownSeconds: request.cooldownSeconds,
          phoneNumber: phoneNumber,
          onBack: () {
            setState(() {
              _step = _VerificationStep.phone;
            });
          },
          onCompleted: _completeVerification,
        );
    }
  }
}
