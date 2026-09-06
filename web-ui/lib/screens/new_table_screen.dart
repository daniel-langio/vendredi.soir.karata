import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'menu_screen.dart';

class NewTableScreen extends StatefulWidget {
  final String serverUrl;
  final String token;
  final String username;

  const NewTableScreen({
    super.key,
    required this.serverUrl,
    required this.token,
    required this.username,
  });

  @override
  State<NewTableScreen> createState() => _NewTableScreenState();
}

class _NewTableScreenState extends State<NewTableScreen> {
  final _nameController = TextEditingController(text: 'Friday Night Poker');
  final _smallBlindController = TextEditingController(text: '10');
  final _bigBlindController = TextEditingController(text: '20');
  final _buyInController = TextEditingController(text: '1000');
  bool _isLoading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _smallBlindController.dispose();
    _bigBlindController.dispose();
    _buyInController.dispose();
    super.dispose();
  }

  Future<void> _create() async {
    final name = _nameController.text.trim();
    final sb = int.tryParse(_smallBlindController.text.trim());
    final bb = int.tryParse(_bigBlindController.text.trim());
    final buyIn = int.tryParse(_buyInController.text.trim());

    if (name.isEmpty || sb == null || sb <= 0 || bb == null || bb <= 0 || buyIn == null || buyIn <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(AppLocalizations.of(context).fillValidValues),
            backgroundColor: KarataColors.red),
      );
      return;
    }

    setState(() => _isLoading = true);
    try {
      final client = ApiClient(baseUrl: widget.serverUrl, token: widget.token);
      final game = await client.createGame(name, sb, bb);
      final gameId = game['gameId'] as String;
      await client.buyIn(gameId, buyIn);
      await saveRecentTable(gameId, name);

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
              content: Text(AppLocalizations.of(context).couldNotCreateTable('$e')),
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
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          children: [
            Text(
              t.newTableTitle,
              style: const TextStyle(
                  fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
            ),
            const SizedBox(height: 8),
            Text(
              t.newTableSubtitle,
              style: const TextStyle(fontSize: 13.5, color: KarataColors.dim, height: 1.45),
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _nameController,
              style: const TextStyle(color: KarataColors.ink),
              decoration: InputDecoration(labelText: t.name),
            ),
            const SizedBox(height: 11),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _smallBlindController,
                    keyboardType: TextInputType.number,
                    style: const TextStyle(color: KarataColors.ink),
                    decoration: InputDecoration(labelText: t.smallBlind),
                  ),
                ),
                const SizedBox(width: 11),
                Expanded(
                  child: TextField(
                    controller: _bigBlindController,
                    keyboardType: TextInputType.number,
                    style: const TextStyle(color: KarataColors.ink),
                    decoration: InputDecoration(labelText: t.bigBlind),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
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
              t.newTableFooter,
              style: const TextStyle(fontSize: 12, color: KarataColors.dim, height: 1.5),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _isLoading ? null : _create,
              child: _isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2, color: KarataColors.ink),
                    )
                  : Text(t.createAndSitDown),
            ),
          ],
        ),
      ),
    );
  }
}
