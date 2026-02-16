import 'package:flutter/material.dart';

import 'core/config/app_config.dart';
import 'core/network/api_client.dart';
import 'core/storage/token_storage.dart';
import 'features/auth/auth_repository.dart';
import 'features/auth/login_page.dart';
import 'features/auth/social_sdk_service.dart';
import 'features/history/history_page.dart';
import 'features/hymn/hymn_detail_page.dart';
import 'features/hymn/hymn_list_page.dart';
import 'features/hymn/hymn_repository.dart';

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
  late final HymnRepository _hymnRepository;

  bool _initializing = true;
  SessionProfile? _profile;
  String? _initError;

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
        });
        return;
      }

      final profile = await _authRepository.fetchProfile();
      if (profile == null) {
        await _authRepository.clearSession();
        _hymnRepository.bindSessionUser(null);
        setState(() {
          _profile = null;
        });
        return;
      }

      _hymnRepository.bindSessionUser(profile.userId);
      setState(() {
        _profile = profile;
      });
      await _hymnRepository.syncPendingActions();
    } catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _initError = e.toString();
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
    _hymnRepository.bindSessionUser(profile.userId);
    setState(() {
      _profile = profile;
    });
    await _hymnRepository.syncPendingActions();
  }

  Future<void> _onLogout() async {
    await _authRepository.logout();
    _hymnRepository.bindSessionUser(null);
    if (!mounted) {
      return;
    }
    setState(() {
      _profile = null;
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
        onLoggedIn: _onLoggedIn,
      );
    }

    return _HomeShell(
      authRepository: _authRepository,
      hymnRepository: _hymnRepository,
      onLogout: _onLogout,
    );
  }
}

class _HomeShell extends StatefulWidget {
  final AuthRepository authRepository;
  final HymnRepository hymnRepository;
  final Future<void> Function() onLogout;

  const _HomeShell({
    required this.authRepository,
    required this.hymnRepository,
    required this.onLogout,
  });

  @override
  State<_HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<_HomeShell> {
  int _index = 0;
  bool _loggingOut = false;

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

  Future<void> _handleLogout() async {
    if (_loggingOut) {
      return;
    }

    setState(() {
      _loggingOut = true;
    });
    try {
      await widget.onLogout();
    } finally {
      if (mounted) {
        setState(() {
          _loggingOut = false;
        });
      }
    }
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
    ];
    final titles = <String>['찬양 둘러보기', '최근 본 찬양'];

    return Scaffold(
      appBar: AppBar(
        title: Text(titles[_index]),
        actions: [
          PopupMenuButton<String>(
            enabled: !_loggingOut,
            onSelected: (value) {
              if (value == 'logout') {
                _handleLogout();
              }
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: 'logout', child: Text('로그아웃')),
            ],
            icon: _loggingOut
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.more_vert),
          ),
        ],
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
            label: '최근 본 항목',
          ),
        ],
      ),
    );
  }
}
