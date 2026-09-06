import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';

class LoginScreen extends StatefulWidget {
  final String serverUrl;

  const LoginScreen({super.key, required this.serverUrl});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final username = _usernameController.text.trim();
    final password = _passwordController.text;

    setState(() => _isLoading = true);
    try {
      final client = ApiClient(baseUrl: widget.serverUrl);
      final token = await client.login(username, password);

      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('server_url', widget.serverUrl);
      await prefs.setString('jwt_token', token);
      await prefs.setString('username', username);

      if (mounted) {
        // Clears the whole stack (not just this screen) - reached via WelcomeScreen's
        // RootScreen, which would otherwise linger below Menu and show as a stray back button.
        Navigator.of(context).pushNamedAndRemoveUntil(
          '/menu',
          (route) => false,
          arguments: {'serverUrl': widget.serverUrl, 'token': token, 'username': username},
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(AppLocalizations.of(context).couldNotLogIn('$e')),
              backgroundColor: KarataColors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  t.logIn,
                  style: const TextStyle(
                      fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
                ),
                const SizedBox(height: 26),
                TextFormField(
                  controller: _usernameController,
                  decoration: InputDecoration(labelText: t.username),
                  validator: (v) => (v == null || v.trim().isEmpty) ? t.required : null,
                ),
                const SizedBox(height: 11),
                TextFormField(
                  controller: _passwordController,
                  obscureText: true,
                  decoration: InputDecoration(labelText: t.password),
                  validator: (v) => (v == null || v.isEmpty) ? t.required : null,
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: _isLoading ? null : _submit,
                  child: _isLoading
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: KarataColors.ink),
                        )
                      : Text(t.logIn),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
