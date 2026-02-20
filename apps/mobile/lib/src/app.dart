import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'core/config/app_config.dart';
import 'core/network/api_client.dart';
import 'core/storage/onboarding_storage.dart';
import 'core/storage/token_storage.dart';
import 'features/auth/auth_repository.dart';
import 'features/auth/auth_verification_flow.dart';
import 'features/auth/login_page.dart';
import 'features/auth/onboarding_page.dart';
import 'features/auth/social_sdk_service.dart';
import 'features/history/history_page.dart';
import 'features/hymn/hymn_detail_page.dart';
import 'features/hymn/hymn_list_page.dart';
import 'features/hymn/hymn_recommendation_page.dart';
import 'features/hymn/hymn_repository.dart';
import 'features/profile/profile_settings_page.dart';

class EunhyeMobileApp extends StatefulWidget {
  const EunhyeMobileApp({super.key});

  @override
  State<EunhyeMobileApp> createState() => _EunhyeMobileAppState();
}

class _EunhyeMobileAppState extends State<EunhyeMobileApp> {
  late final TokenStorage _tokenStorage;
  late final ApiClient _apiClient;
  late final AuthRepository _authRepository;
  late final SocialSdkService _socialSdkService;
  late final OnboardingStorage _onboardingStorage;
  late final HymnRepository _hymnRepository;

  bool _initializing = true;
  SessionProfile? _profile;
  String? _initError;
  String? _loginError;
  bool _needsOnboarding = false;

  @override
  void initState() {
    super.initState();
    _tokenStorage = TokenStorage();
    _apiClient = ApiClient(
      baseUrl: AppConfig.apiBaseUrl,
      tokenStorage: _tokenStorage,
    );
    _authRepository = AuthRepository(
      apiClient: _apiClient,
      tokenStorage: _tokenStorage,
    );
    _socialSdkService = SocialSdkService()..initialize();
    _onboardingStorage = OnboardingStorage();
    _hymnRepository = HymnRepository(apiClient: _apiClient);
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    setState(() {
      _initializing = true;
      _initError = null;
    });

    try {
      final hasSession = await _authRepository.hasSession();
      if (!hasSession) {
        _hymnRepository.bindSessionUser(null);
        setState(() {
          _profile = null;
          _needsOnboarding = false;
          _loginError = null;
        });
        return;
      }

      final profile = await _authRepository.fetchProfile();
      if (profile == null) {
        await _authRepository.clearSession();
        _hymnRepository.bindSessionUser(null);
        setState(() {
          _profile = null;
          _needsOnboarding = false;
          _loginError = null;
        });
        return;
      }

      final resolved = await _resolveProfile(profile);
      _hymnRepository.bindSessionUser(resolved.profile.userId);
      setState(() {
        _profile = resolved.profile;
        _needsOnboarding = resolved.needsOnboarding;
        _loginError = null;
      });
      await _hymnRepository.syncPendingActions();
    } catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _initError = e.toString();
        _loginError = null;
      });
    } finally {
      if (mounted) {
        setState(() {
          _initializing = false;
        });
      }
    }
  }

  Future<void> _onLoggedIn(SessionProfile profile) async {
    final resolved = await _resolveProfile(profile);
    _hymnRepository.bindSessionUser(resolved.profile.userId);
    setState(() {
      _profile = resolved.profile;
      _needsOnboarding = resolved.needsOnboarding;
      _loginError = null;
    });
    await _hymnRepository.syncPendingActions();
  }

  Future<void> _onVerificationCompleted(SessionProfile profile) async {
    final resolved = await _resolveProfile(profile);
    _hymnRepository.bindSessionUser(resolved.profile.userId);
    setState(() {
      _profile = resolved.profile;
      _needsOnboarding = resolved.needsOnboarding;
      _loginError = null;
    });
    await _hymnRepository.syncPendingActions();
  }

  Future<void> _onOnboardingCompleted(SessionProfile profile) async {
    if (!mounted) {
      return;
    }
    _hymnRepository.bindSessionUser(profile.userId);
    await _hymnRepository.syncPendingActions();
    setState(() {
      _profile = profile;
      _needsOnboarding = false;
    });
  }

  Future<void> _onReturnToLogin() async {
    _hymnRepository.bindSessionUser(null);
    if (!mounted) {
      return;
    }
    setState(() {
      _profile = null;
      _needsOnboarding = false;
      _loginError = null;
    });
  }

  Future<_ProfileResolution> _resolveProfile(SessionProfile profile) async {
    var effective = profile;
    if (profile.verified && !profile.isProfileCompleted) {
      final local = await _onboardingStorage.getProfile(profile.userId);
      if (local != null) {
        try {
          final synced = await _authRepository.updateProfile(
            churchName: local.churchName,
            name: local.name,
            group: local.group,
            gender: _toUserGender(local.gender),
          );
          effective = synced;
        } catch (_) {
          // Keep onboarding required when server sync is unavailable.
        }
      }
    }

    return _ProfileResolution(
      profile: effective,
      needsOnboarding: effective.verified && !effective.isProfileCompleted,
    );
  }

  UserGender _toUserGender(String raw) {
    return switch (raw.trim().toUpperCase()) {
      'MALE' => UserGender.male,
      'FEMALE' => UserGender.female,
      _ => UserGender.unknown,
    };
  }

  void _onProfileUpdated(SessionProfile profile) {
    if (!mounted) {
      return;
    }
    setState(() {
      _profile = profile;
      _needsOnboarding = profile.verified && !profile.isProfileCompleted;
    });
  }

  Future<void> _onLogout() async {
    await _authRepository.logout();
    _hymnRepository.bindSessionUser(null);
    if (!mounted) {
      return;
    }
    setState(() {
      _profile = null;
      _needsOnboarding = false;
      _loginError = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '은혜찬송',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFFF6F0F)),
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFFF6F7F9),
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.white,
          foregroundColor: Color(0xFF1F2937),
          elevation: 0,
          surfaceTintColor: Colors.transparent,
        ),
        cardTheme: CardThemeData(
          color: Colors.white,
          elevation: 0,
          margin: EdgeInsets.zero,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Colors.white,
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          labelStyle: const TextStyle(color: Color(0xFF4B5563)),
          hintStyle: const TextStyle(color: Color(0xFF9CA3AF)),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Color(0xFFE5E7EB)),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Color(0xFFE5E7EB)),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Color(0xFFFB923C), width: 1.2),
          ),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            minimumSize: const Size.fromHeight(48),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            minimumSize: const Size.fromHeight(48),
            side: const BorderSide(color: Color(0xFFD1D5DB)),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
      ),
      home: _buildHome(),
    );
  }

  Widget _buildHome() {
    if (_initializing) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    if (_initError != null) {
      return Scaffold(
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(_initError!, style: const TextStyle(color: Colors.red)),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: _bootstrap,
                  child: const Text('다시 시도'),
                ),
              ],
            ),
          ),
        ),
      );
    }

    if (_profile == null) {
      return LoginPage(
        authRepository: _authRepository,
        socialSdkService: _socialSdkService,
        initialError: _loginError,
        onLoggedIn: _onLoggedIn,
      );
    }

    if (!_profile!.verified) {
      return AuthVerificationFlow(
        authRepository: _authRepository,
        profile: _profile!,
        onVerified: _onVerificationCompleted,
        onReturnToLogin: _onReturnToLogin,
      );
    }

    if (_needsOnboarding) {
      return OnboardingPage(
        userId: _profile!.userId,
        authRepository: _authRepository,
        onboardingStorage: _onboardingStorage,
        onCompleted: _onOnboardingCompleted,
      );
    }

    return _HomeShell(
      initialProfile: _profile!,
      authRepository: _authRepository,
      hymnRepository: _hymnRepository,
      onboardingStorage: _onboardingStorage,
      onProfileUpdated: _onProfileUpdated,
      onLogout: _onLogout,
    );
  }
}

class _HomeShell extends StatefulWidget {
  final SessionProfile initialProfile;
  final AuthRepository authRepository;
  final HymnRepository hymnRepository;
  final OnboardingStorage onboardingStorage;
  final ValueChanged<SessionProfile> onProfileUpdated;
  final Future<void> Function() onLogout;

  const _HomeShell({
    required this.initialProfile,
    required this.authRepository,
    required this.hymnRepository,
    required this.onboardingStorage,
    required this.onProfileUpdated,
    required this.onLogout,
  });

  @override
  State<_HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<_HomeShell> {
  static const _exitSnackBarDuration = Duration(seconds: 2);

  final _scaffoldKey = GlobalKey<ScaffoldState>();
  int _index = 0;
  DateTime? _lastBackPressedAt;
  late SessionProfile _profile;

  @override
  void initState() {
    super.initState();
    _profile = widget.initialProfile;
  }

  @override
  void didUpdateWidget(covariant _HomeShell oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.initialProfile != widget.initialProfile) {
      _profile = widget.initialProfile;
    }
  }

  Future<void> _openHymnDetail(String hymnId) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => HymnDetailPage(
          hymnId: hymnId,
          hymnRepository: widget.hymnRepository,
        ),
      ),
    );
  }

  void _handleProfileUpdated(SessionProfile profile) {
    if (!mounted) {
      return;
    }
    setState(() {
      _profile = profile;
    });
    widget.onProfileUpdated(profile);
  }

  String get _memberLabel {
    final suffix = switch (_profile.gender) {
      UserGender.male => '형제',
      UserGender.female => '자매',
      UserGender.unknown => '형제/자매',
    };
    final name = _profile.name?.trim();
    if (name != null && name.isNotEmpty) {
      return '$name $suffix';
    }

    final displayName = _profile.displayName?.trim();
    if (displayName != null && displayName.isNotEmpty) {
      return '$displayName $suffix';
    }
    return '이름 미등록';
  }

  String get _churchLabel {
    final churchName = _profile.churchName?.trim();
    if (churchName == null || churchName.isEmpty) {
      return '교회 정보 없음';
    }
    return churchName;
  }

  void _openDrawer() {
    _scaffoldKey.currentState?.openDrawer();
  }

  void _changeSection(int index) {
    Navigator.of(context).pop();
    if (_index == index) {
      return;
    }
    setState(() {
      _index = index;
    });
  }

  void _openAiTab() {
    if (_index == 1) {
      return;
    }
    setState(() {
      _index = 1;
    });
  }

  Future<void> _handleSystemBack() async {
    final scaffoldState = _scaffoldKey.currentState;
    if (scaffoldState?.isDrawerOpen ?? false) {
      Navigator.of(context).pop();
      return;
    }

    final now = DateTime.now();
    if (_lastBackPressedAt != null &&
        now.difference(_lastBackPressedAt!) <= _exitSnackBarDuration) {
      await SystemNavigator.pop();
      return;
    }

    _lastBackPressedAt = now;
    if (!mounted) {
      return;
    }

    final messenger = ScaffoldMessenger.of(context);
    messenger.hideCurrentSnackBar();
    messenger.showSnackBar(
      const SnackBar(
        content: Text('뒤로 버튼을 한번 더 누르시면 종료됩니다.'),
        duration: _exitSnackBarDuration,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      HymnListPage(
        hymnRepository: widget.hymnRepository,
        onOpenHymnDetail: _openHymnDetail,
        onOpenRecommendations: _openAiTab,
      ),
      HymnRecommendationPage(
        hymnRepository: widget.hymnRepository,
        onOpenHymnDetail: _openHymnDetail,
      ),
      HistoryPage(
        hymnRepository: widget.hymnRepository,
        onOpenHymnDetail: _openHymnDetail,
      ),
      ProfileSettingsPage(
        profile: _profile,
        authRepository: widget.authRepository,
        hymnRepository: widget.hymnRepository,
        onboardingStorage: widget.onboardingStorage,
        onProfileUpdated: _handleProfileUpdated,
        onLogout: widget.onLogout,
      ),
    ];

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) {
          return;
        }
        await _handleSystemBack();
      },
      child: Scaffold(
        key: _scaffoldKey,
        appBar: AppBar(
          automaticallyImplyLeading: false,
          toolbarHeight: 48,
          scrolledUnderElevation: 0,
          leading: IconButton(
            onPressed: _openDrawer,
            icon: const Icon(Icons.menu),
            tooltip: '메뉴 열기',
          ),
        ),
        drawer: Drawer(
          child: SafeArea(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Container(
                  padding: const EdgeInsets.fromLTRB(20, 18, 20, 16),
                  decoration: const BoxDecoration(
                    color: Color(0xFFF8FAFC),
                    border: Border(
                      bottom: BorderSide(color: Color(0xFFE5E7EB)),
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const CircleAvatar(
                        radius: 22,
                        backgroundColor: Color(0xFFFFEDD5),
                        child: Icon(Icons.person, color: Color(0xFFEA580C)),
                      ),
                      const SizedBox(height: 10),
                      Text(
                        _memberLabel,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: Color(0xFF111827),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _churchLabel,
                        style: const TextStyle(
                          color: Color(0xFF4B5563),
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
                _DrawerMenuTile(
                  icon: Icons.library_music_outlined,
                  label: '찬양',
                  selected: _index == 0,
                  onTap: () => _changeSection(0),
                ),
                _DrawerMenuTile(
                  icon: Icons.auto_awesome_outlined,
                  label: 'AI 추천',
                  selected: _index == 1,
                  onTap: () => _changeSection(1),
                ),
                _DrawerMenuTile(
                  icon: Icons.history_outlined,
                  label: '히스토리',
                  selected: _index == 2,
                  onTap: () => _changeSection(2),
                ),
                _DrawerMenuTile(
                  icon: Icons.settings_outlined,
                  label: '설정',
                  selected: _index == 3,
                  onTap: () => _changeSection(3),
                ),
              ],
            ),
          ),
        ),
        body: IndexedStack(
          index: _index,
          children: pages,
        ),
      ),
    );
  }
}

class _DrawerMenuTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _DrawerMenuTile({
    required this.icon,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final itemColor =
        selected ? const Color(0xFFEA580C) : const Color(0xFF374151);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 2),
      child: ListTile(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        selected: selected,
        selectedTileColor: const Color(0xFFFFF7ED),
        leading: Icon(icon, color: itemColor),
        title: Text(
          label,
          style: TextStyle(
            color: itemColor,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w600,
          ),
        ),
        onTap: onTap,
      ),
    );
  }
}

class _ProfileResolution {
  final SessionProfile profile;
  final bool needsOnboarding;

  const _ProfileResolution({
    required this.profile,
    required this.needsOnboarding,
  });
}
