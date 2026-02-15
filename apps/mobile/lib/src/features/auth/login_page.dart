import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../../core/network/api_exception.dart';
import 'auth_repository.dart';
import 'social_sdk_service.dart';

class LoginPage extends StatefulWidget {
  final AuthRepository authRepository;
  final SocialSdkService socialSdkService;
  final Future<void> Function(SessionProfile profile) onLoggedIn;

  const LoginPage({
    super.key,
    required this.authRepository,
    required this.socialSdkService,
    required this.onLoggedIn,
  });

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _inviteCodeController = TextEditingController();
  final _displayNameController = TextEditingController(text: 'Mobile User');

  UserRole _devRole = UserRole.user;
  bool _loading = false;
  bool _showDevLogin = false;
  String? _error;

  @override
  void dispose() {
    _inviteCodeController.dispose();
    _displayNameController.dispose();
    super.dispose();
  }

  Future<void> _handleSocialLogin() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final token = await widget.socialSdkService.fetchToken();
      final profile = await widget.authRepository.loginWithSocial(
        token: token,
        inviteCode: _inviteCodeController.text.trim().isEmpty
            ? null
            : _inviteCodeController.text.trim(),
      );
      await widget.onLoggedIn(profile);
    } on ApiException catch (e) {
      setState(() {
        _error = e.message;
      });
    } catch (e) {
      setState(() {
        _error = 'Social login failed: $e';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  Future<void> _handleDevLogin() async {
    if (_displayNameController.text.trim().isEmpty) {
      setState(() {
        _error = 'Display name is required.';
      });
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final profile = await widget.authRepository.loginWithDev(
        displayName: _displayNameController.text.trim(),
        userId: const Uuid().v4(),
        role: _devRole,
      );
      await widget.onLoggedIn(profile);
    } on ApiException catch (e) {
      setState(() {
        _error = e.message;
      });
    } catch (e) {
      setState(() {
        _error = 'Dev login failed: $e';
      });
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
        title: const Text('Eunhye Hymn Login'),
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Text(
                        _error!,
                        style: const TextStyle(color: Colors.red),
                      ),
                    ),
                  const Text(
                    'Social Login',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    'Tap Kakao icon to sign in with provider redirect.',
                  ),
                  const SizedBox(height: 12),
                  _SocialLoginButton(
                    onPressed: _loading ? null : _handleSocialLogin,
                    label: _loading ? 'Signing in...' : 'Continue with Kakao',
                    backgroundColor: const Color(0xFFFEE500),
                    foregroundColor: Colors.black87,
                    borderColor: const Color(0xFFFEE500),
                    icon: const Icon(Icons.chat_bubble_rounded),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _inviteCodeController,
                    enabled: !_loading,
                    decoration: const InputDecoration(
                      labelText: 'Invite code (first login only)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 20),
                  OutlinedButton.icon(
                    onPressed: _loading
                        ? null
                        : () {
                            setState(() => _showDevLogin = !_showDevLogin);
                          },
                    icon: Icon(
                        _showDevLogin ? Icons.expand_less : Icons.expand_more),
                    label: const Text('Dev Login (local only)'),
                  ),
                  if (_showDevLogin) ...[
                    const SizedBox(height: 12),
                    TextField(
                      controller: _displayNameController,
                      enabled: !_loading,
                      decoration: const InputDecoration(
                        labelText: 'Display name',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<UserRole>(
                      initialValue: _devRole,
                      decoration: const InputDecoration(
                        labelText: 'Role',
                        border: OutlineInputBorder(),
                      ),
                      items: const [
                        DropdownMenuItem(
                          value: UserRole.user,
                          child: Text('USER'),
                        ),
                        DropdownMenuItem(
                          value: UserRole.admin,
                          child: Text('ADMIN'),
                        ),
                      ],
                      onChanged: _loading
                          ? null
                          : (value) {
                              if (value != null) {
                                setState(() => _devRole = value);
                              }
                            },
                    ),
                    const SizedBox(height: 12),
                    FilledButton.tonal(
                      onPressed: _loading ? null : _handleDevLogin,
                      child: Text(_loading ? 'Processing...' : 'Dev Login'),
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

class _SocialLoginButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final String label;
  final Color backgroundColor;
  final Color foregroundColor;
  final Color borderColor;
  final Widget icon;

  const _SocialLoginButton({
    required this.onPressed,
    required this.label,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.borderColor,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 52,
      child: FilledButton.icon(
        onPressed: onPressed,
        icon: icon,
        label: Text(
          label,
          textAlign: TextAlign.center,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        style: FilledButton.styleFrom(
          backgroundColor: backgroundColor,
          foregroundColor: foregroundColor,
          side: BorderSide(color: borderColor),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }
}
