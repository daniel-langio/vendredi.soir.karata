import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'menu_screen.dart';

final _uuidPattern = RegExp(
    r'[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}');

class JoinTableScreen extends StatefulWidget {
  final String serverUrl;
  final String token;
  final String username;

  const JoinTableScreen({
    super.key,
    required this.serverUrl,
    required this.token,
    required this.username,
  });

  @override
  State<JoinTableScreen> createState() => _JoinTableScreenState();
}

class _JoinTableScreenState extends State<JoinTableScreen> {
  final _linkController = TextEditingController();
  final _buyInController = TextEditingController(text: '1000');

  bool _isLoading = false;
  Map<String, dynamic>? _preview;
  bool _alreadySeated = false;

  @override
  void dispose() {
    _linkController.dispose();
    _buyInController.dispose();
    super.dispose();
  }

  String? _extractGameId() {
    final match = _uuidPattern.firstMatch(_linkController.text);
    return match?.group(0);
  }

  Future<void> _lookUp() async {
    final gameId = _extractGameId();
    if (gameId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(AppLocalizations.of(context).noValidLink),
            backgroundColor: KarataColors.red),
      );
      return;
    }

    setState(() => _isLoading = true);
    try {
      final client = ApiClient(baseUrl: widget.serverUrl, token: widget.token);
      final game = await client.getGame(gameId);
      final players = (game['players'] as List<dynamic>? ?? []);
      setState(() {
        _preview = game;
        _alreadySeated = players.any((p) => p['username'] == widget.username);
      });
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(AppLocalizations.of(context).tableNotFound('$e')),
              backgroundColor: KarataColors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _sitDown() async {
    final gameId = _preview!['gameId'] as String;
    final client = ApiClient(baseUrl: widget.serverUrl, token: widget.token);

    setState(() => _isLoading = true);
    try {
      if (!_alreadySeated) {
        final buyIn = int.tryParse(_buyInController.text.trim());
        if (buyIn == null || buyIn <= 0) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
                content: Text(AppLocalizations.of(context).enterValidBuyIn),
                backgroundColor: KarataColors.red),
          );
          return;
        }
        await client.buyIn(gameId, buyIn);
      }
      await saveRecentTable(gameId, _preview!['name'] as String? ?? 'Table');

      if (mounted) {
        Navigator.of(context).pushReplacementNamed(
          '/table/$gameId',
          arguments: {
            'serverUrl': widget.serverUrl,
            'token': widget.token,
            'username': widget.username,
          },
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(AppLocalizations.of(context).couldNotSitDown('$e')),
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
    final players = (_preview?['players'] as List<dynamic>? ?? []);
    return Scaffold(
      appBar: AppBar(),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          children: [
            Text(
              t.joinTableTitle,
              style: const TextStyle(
                  fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
            ),
            const SizedBox(height: 8),
            Text(
              t.joinTableSubtitle,
              style: const TextStyle(fontSize: 13.5, color: KarataColors.dim, height: 1.45),
            ),
            const SizedBox(height: 24),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _linkController,
                    style: const TextStyle(color: KarataColors.ink, fontSize: 13),
                    decoration: InputDecoration(hintText: t.linkHint),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _isLoading ? null : _lookUp,
                  style: ElevatedButton.styleFrom(minimumSize: const Size(0, 56)),
                  child: Text(t.find),
                ),
              ],
            ),
            if (_preview != null) ...[
              const SizedBox(height: 24),
              Text(t.foundIt,
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
              const SizedBox(height: 8),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.circle, size: 7, color: KarataColors.live),
                title: Text(_preview!['name'] as String? ?? '',
                    style: const TextStyle(color: KarataColors.ink, fontSize: 16.5)),
                subtitle: Text(
                  t.blindsSeated('${_preview!['blinds']?['small']}',
                      '${_preview!['blinds']?['big']}', players.length),
                  style: const TextStyle(color: KarataColors.dim, fontSize: 12.5),
                ),
              ),
              if (!_alreadySeated) ...[
                const SizedBox(height: 20),
                Text(t.yourBuyIn,
                    style: const TextStyle(
                        fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
                const SizedBox(height: 12),
                TextField(
                  controller: _buyInController,
                  keyboardType: TextInputType.number,
                  style: const TextStyle(color: KarataColors.ink),
                  decoration: InputDecoration(labelText: t.chips),
                ),
                const SizedBox(height: 14),
                Text(
                  t.youCanOnlyBuyInOnce,
                  style: const TextStyle(fontSize: 12, color: KarataColors.dim, height: 1.5),
                ),
              ],
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _isLoading ? null : _sitDown,
                child: _isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: KarataColors.ink),
                      )
                    : Text(_alreadySeated ? t.openTable : t.sitDown),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
