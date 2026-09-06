import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';

const kDefaultServerUrl =
    'https://62zx5a4vo6n3zjykzu7dx3a4zy0imiwo.lambda-url.eu-west-3.on.aws/poker';

/// When this app is served from the same Spring Boot app it talks to (the
/// intended deployment for the web build - see web-ui/README.md), same-origin
/// requests need no CORS at all, so default to wherever this page itself was
/// loaded from rather than the hardcoded Lambda URL used by the native builds.
String defaultServerUrl() {
  if (kIsWeb) {
    return '${Uri.base.origin}/poker';
  }
  return kDefaultServerUrl;
}

class WelcomeScreen extends StatefulWidget {
  const WelcomeScreen({super.key});

  @override
  State<WelcomeScreen> createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  final _urlController = TextEditingController(text: defaultServerUrl());
  bool _showServerField = false;

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  void _goToRegister() {
    final serverUrl = _urlController.text.trim();
    Navigator.of(context).pushNamed('/register', arguments: {'serverUrl': serverUrl});
  }

  void _goToLogin() {
    final serverUrl = _urlController.text.trim();
    Navigator.of(context).pushNamed('/login', arguments: {'serverUrl': serverUrl});
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 40, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              const Icon(Icons.casino, size: 72, color: KarataColors.ink),
              const SizedBox(height: 16),
              const Text(
                'Karata',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
              ),
              const SizedBox(height: 8),
              Text(
                t.welcomeTagline,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 13.5, color: KarataColors.dim, height: 1.45),
              ),
              const Spacer(),
              if (_showServerField) ...[
                TextField(
                  controller: _urlController,
                  style: const TextStyle(color: KarataColors.ink, fontSize: 13),
                  decoration: InputDecoration(labelText: t.serverBaseUrl),
                ),
                const SizedBox(height: 16),
              ],
              ElevatedButton(onPressed: _goToRegister, child: Text(t.createAccount)),
              const SizedBox(height: 11),
              OutlinedButton(onPressed: _goToLogin, child: Text(t.logIn)),
              const SizedBox(height: 8),
              TextButton(
                onPressed: () => setState(() => _showServerField = !_showServerField),
                child: Text(_showServerField ? t.hideServerSettings : t.serverSettings),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
