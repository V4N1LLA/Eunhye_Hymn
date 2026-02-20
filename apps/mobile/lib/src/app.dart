import 'package:flutter/material.dart';

import 'core/config/app_config.dart';
import 'core/network/api_client.dart';
import 'core/storage/onboarding_storage.dart';
import 'core/storage/token_storage.dart';
import 'features/auth/auth_repository.dart';
import 'features/auth/login_page.dart';
import 'features/auth/onboarding_page.dart';
import 'features/auth/social_sdk_service.dart';
import 'features/history/history_page.dart';
import 'features/hymn/hymn_detail_page.dart';
import 'features/hymn/hymn_list_page.dart';
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

  Future<_ProfileResolution> _resolveProfile(SessionProfile profile) async {
    var effective = profile;
    if (!profile.isProfileCompleted) {
      final local = await _onboardingStorage.getProfile(profile.userId);
      if (local != null) {
        try {
          final synced = await _authRepository.updateProfile(
            churchName: local.churchName,
            name: local.name,
            group: local.group,
          );
          effective = synced;
        } catch (_) {
          // Keep onboarding required when server sync is unavailable.
        }
      }
    }

    return _ProfileResolution(
      profile: effective,
      needsOnboarding: !effective.isProfileCompleted,
    );
  }

  void _onProfileUpdated(SessionProfile profile) {
    if (!mounted) {
      return;
    }
    setState(() {
      _profile = profile;
      _needsOnboarding = !profile.isProfileCompleted;
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
  int _index = 0;
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

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      HymnListPage(
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
    final titles = <String>['찬양', '히스토리', '내 정보'];

    return Scaffold(
      appBar: AppBar(
        title: Text(titles[_index]),
      ),
      body: pages[_index],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (index) {
          setState(() {
            _index = index;
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.library_music_outlined),
            selectedIcon: Icon(Icons.library_music),
            label: '찬양',
          ),
          NavigationDestination(
            icon: Icon(Icons.history_outlined),
            selectedIcon: Icon(Icons.history),
            label: '히스토리',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: '내 정보',
          ),
        ],
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
