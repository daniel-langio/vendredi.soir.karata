import 'package:flutter/material.dart';
import '../api/api_client.dart';
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
        const SnackBar(content: Text('Please fill in valid values'), backgroundColor: KarataColors.red),
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
          SnackBar(content: Text('Could not create table: $e'), backgroundColor: KarataColors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          children: [
            const Text(
              'New table',
              style: TextStyle(fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
            ),
            const SizedBox(height: 8),
            const Text(
              'Name and blinds are all the server keeps. Everything else is set when '
              'each player sits down.',
              style: TextStyle(fontSize: 13.5, color: KarataColors.dim, height: 1.45),
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _nameController,
              style: const TextStyle(color: KarataColors.ink),
              decoration: const InputDecoration(labelText: 'Name'),
            ),
            const SizedBox(height: 11),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _smallBlindController,
                    keyboardType: TextInputType.number,
                    style: const TextStyle(color: KarataColors.ink),
                    decoration: const InputDecoration(labelText: 'Small blind'),
                  ),
                ),
                const SizedBox(width: 11),
                Expanded(
                  child: TextField(
                    controller: _bigBlindController,
                    keyboardType: TextInputType.number,
                    style: const TextStyle(color: KarataColors.ink),
                    decoration: const InputDecoration(labelText: 'Big blind'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            const Text('Your buy-in',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
            const SizedBox(height: 12),
            TextField(
              controller: _buyInController,
              keyboardType: TextInputType.number,
              style: const TextStyle(color: KarataColors.ink),
              decoration: const InputDecoration(labelText: 'Chips'),
            ),
            const SizedBox(height: 14),
            const Text(
              'Creating the table seats you at it. Everyone else picks their own buy-in '
              'when they join, and chips carry between hands.',
              style: TextStyle(fontSize: 12, color: KarataColors.dim, height: 1.5),
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
                  : const Text('Create and sit down'),
            ),
          ],
        ),
      ),
    );
  }
}
