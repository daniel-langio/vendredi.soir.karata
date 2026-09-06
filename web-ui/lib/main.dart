import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'l10n/app_localizations.dart';
import 'locale_controller.dart';
import 'screens/welcome_screen.dart';
import 'screens/register_screen.dart';
import 'screens/login_screen.dart';
import 'screens/menu_screen.dart';
import 'screens/new_table_screen.dart';
import 'screens/join_table_screen.dart';
import 'screens/table_screen.dart';
import 'theme.dart';

void main() {
  // Drop the "#" from URLs (e.g. karata.example.com/table/<id> instead of
  // karata.example.com/#/table/<id>) so paths look like normal web app paths.
  usePathUrlStrategy();
  LocaleController.instance.load();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<Locale?>(
      valueListenable: LocaleController.instance,
      builder: (context, locale, _) {
        return MaterialApp(
          title: 'Karata',
          debugShowCheckedModeBanner: false,
          theme: karataTheme(),
          locale: locale,
          supportedLocales: AppLocalizations.supportedLocales,
          localizationsDelegates: const [
            AppLocalizations.delegate,
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
          ],
          onGenerateRoute: _onGenerateRoute,
          // This UI was designed as a phone screen, not a desktop page - on a wide browser window
          // it just looked like that same phone layout stretched edge to edge. Cap it at a
          // phone-ish width and center it instead, same as most mobile-first web apps do.
          builder: (context, child) => ColoredBox(
            color: KarataColors.bg,
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 430),
                child: child,
              ),
            ),
          ),
        );
      },
    );
  }
}

/// Session data a screen needs, either handed down via route arguments during
/// normal in-app navigation, or reloaded from shared_preferences when the
/// route is entered directly (a deep link, or a browser refresh).
class _Session {
  final String serverUrl;
  final String token;
  final String username;
  const _Session({required this.serverUrl, required this.token, required this.username});

  static _Session? fromArguments(Object? arguments) {
    if (arguments is! Map) return null;
    final serverUrl = arguments['serverUrl'];
    final token = arguments['token'];
    final username = arguments['username'];
    if (serverUrl is! String || token is! String || username is! String) return null;
    return _Session(serverUrl: serverUrl, token: token, username: username);
  }
}

Route<dynamic>? _onGenerateRoute(RouteSettings settings) {
  final uri = Uri.parse(settings.name ?? '/');
  final segments = uri.pathSegments;
  final session = _Session.fromArguments(settings.arguments);

  Widget page;
  if (segments.isEmpty) {
    page = const RootScreen();
  } else if (segments.length == 1 && segments[0] == 'register') {
    final args = settings.arguments as Map?;
    page = RegisterScreen(serverUrl: (args?['serverUrl'] as String?) ?? defaultServerUrl());
  } else if (segments.length == 1 && segments[0] == 'login') {
    final args = settings.arguments as Map?;
    page = LoginScreen(serverUrl: (args?['serverUrl'] as String?) ?? defaultServerUrl());
  } else if (segments.length == 1 && segments[0] == 'menu') {
    page = session == null
        ? const RootScreen()
        : MenuScreen(serverUrl: session.serverUrl, token: session.token, username: session.username);
  } else if (segments.length == 1 && segments[0] == 'new-table') {
    page = session == null
        ? const RootScreen()
        : NewTableScreen(
            serverUrl: session.serverUrl, token: session.token, username: session.username);
  } else if (segments.length == 1 && segments[0] == 'join-table') {
    page = session == null
        ? const RootScreen()
        : JoinTableScreen(
            serverUrl: session.serverUrl, token: session.token, username: session.username);
  } else if (segments.length == 2 && segments[0] == 'table') {
    final gameId = segments[1];
    page = session == null
        ? _TableRouteLoader(gameId: gameId)
        : TableScreen(
            serverUrl: session.serverUrl,
            token: session.token,
            username: session.username,
            gameId: gameId,
          );
  } else {
    page = const RootScreen();
  }

  return MaterialPageRoute(settings: settings, builder: (context) => page);
}

/// Loads a saved session before entering a table reached directly (a deep
/// link someone was sent, or a browser refresh) rather than via in-app
/// navigation, where the session would already be in the route's arguments.
class _TableRouteLoader extends StatefulWidget {
  final String gameId;
  const _TableRouteLoader({required this.gameId});

  @override
  State<_TableRouteLoader> createState() => _TableRouteLoaderState();
}

class _TableRouteLoaderState extends State<_TableRouteLoader> {
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    final serverUrl = prefs.getString('server_url');
    final token = prefs.getString('jwt_token');
    final username = prefs.getString('username');
    if (!mounted) return;

    if (serverUrl != null && serverUrl.isNotEmpty && token != null && token.isNotEmpty && username != null) {
      Navigator.of(context).pushReplacementNamed(
        '/table/${widget.gameId}',
        arguments: {'serverUrl': serverUrl, 'token': token, 'username': username},
      );
    } else {
      Navigator.of(context).pushReplacementNamed('/');
    }
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: CircularProgressIndicator()));
  }
}

/// Decides whether a saved session exists before the user sees anything.
class RootScreen extends StatefulWidget {
  const RootScreen({super.key});

  @override
  State<RootScreen> createState() => _RootScreenState();
}

class _RootScreenState extends State<RootScreen> {
  bool _hasSession = false;
  bool _checked = false;

  @override
  void initState() {
    super.initState();
    _decide();
  }

  Future<void> _decide() async {
    final prefs = await SharedPreferences.getInstance();
    final serverUrl = prefs.getString('server_url');
    final token = prefs.getString('jwt_token');
    final username = prefs.getString('username');

    if (!mounted) return;

    if (serverUrl != null && serverUrl.isNotEmpty && token != null && token.isNotEmpty && username != null) {
      setState(() => _hasSession = true);
      Navigator.of(context).pushNamedAndRemoveUntil(
        '/menu',
        (route) => false,
        arguments: {'serverUrl': serverUrl, 'token': token, 'username': username},
      );
    } else {
      setState(() => _checked = true);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_hasSession || !_checked) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return const WelcomeScreen();
  }
}
