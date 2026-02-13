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
    _socialSdkService = SocialSdkService();
    _socialSdkService.initialize();
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
        setState(() {
          _profile = null;
        });
        return;
      }

      final profile = await _authRepository.fetchProfile();
      if (profile == null) {
        await _authRepository.clearSession();
        setState(() {
          _profile = null;
        });
        return;
      }

      setState(() {
        _profile = profile;
      });
      await _hymnRepository.syncPendingActions();
    } catch (e) {
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
    setState(() {
      _profile = profile;
    });
    await _hymnRepository.syncPendingActions();
  }

  Future<void> _onLogout() async {
    await _authRepository.logout();
    await _socialSdkService.signOutGoogle();
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
      title: 'Eunhye Hymn',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
        useMaterial3: true,
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
            padding: const EdgeInsets.all(16),
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
      profile: _profile!,
      authRepository: _authRepository,
      hymnRepository: _hymnRepository,
      onLogout: _onLogout,
    );
  }
}

class _HomeShell extends StatefulWidget {
  final SessionProfile profile;
  final AuthRepository authRepository;
  final HymnRepository hymnRepository;
  final Future<void> Function() onLogout;

  const _HomeShell({
    required this.profile,
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

    final titles = ['찬양 목록', '최근 열람'];
    final roleLabel = widget.profile.role == UserRole.admin ? 'ADMIN' : 'USER';

    return Scaffold(
      appBar: AppBar(
        title: Text('${titles[_index]} · $roleLabel'),
        actions: [
          IconButton(
            onPressed: _loggingOut ? null : _handleLogout,
            icon: const Icon(Icons.logout),
            tooltip: '로그아웃',
          ),
        ],
      ),
      body: pages[_index],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (index) {
          setState(() => _index = index);
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
        ],
      ),
    );
  }
}
