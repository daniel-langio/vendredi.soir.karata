import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../theme.dart';
import 'menu_screen.dart';
import 'table_screen.dart';

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
        const SnackBar(content: Text('No valid table link found'), backgroundColor: KarataColors.red),
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
          SnackBar(content: Text('Table not found: $e'), backgroundColor: KarataColors.red),
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
            const SnackBar(content: Text('Enter a valid buy-in'), backgroundColor: KarataColors.red),
          );
          return;
        }
        await client.buyIn(gameId, buyIn);
      }
      await saveRecentTable(gameId, _preview!['name'] as String? ?? 'Table');

      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (context) => TableScreen(
              serverUrl: widget.serverUrl,
              token: widget.token,
              username: widget.username,
              gameId: gameId,
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Could not sit down: $e'), backgroundColor: KarataColors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final players = (_preview?['players'] as List<dynamic>? ?? []);
    return Scaffold(
      appBar: AppBar(),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          children: [
            const Text(
              'Join a table',
              style: TextStyle(fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
            ),
            const SizedBox(height: 8),
            const Text(
              "Paste the link someone sent you. Table IDs are long, so nobody should ever "
              'have to read one out.',
              style: TextStyle(fontSize: 13.5, color: KarataColors.dim, height: 1.45),
            ),
            const SizedBox(height: 24),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _linkController,
                    style: const TextStyle(color: KarataColors.ink, fontSize: 13),
                    decoration: const InputDecoration(hintText: 'karata.app/t/…'),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _isLoading ? null : _lookUp,
                  style: ElevatedButton.styleFrom(minimumSize: const Size(0, 56)),
                  child: const Text('Find'),
                ),
              ],
            ),
            if (_preview != null) ...[
              const SizedBox(height: 24),
              const Text('Found it',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
              const SizedBox(height: 8),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.circle, size: 7, color: KarataColors.live),
                title: Text(_preview!['name'] as String? ?? '',
                    style: const TextStyle(color: KarataColors.ink, fontSize: 16.5)),
                subtitle: Text(
                  'Blinds ${_preview!['blinds']?['small']} / ${_preview!['blinds']?['big']} · '
                  '${players.length} seated',
                  style: const TextStyle(color: KarataColors.dim, fontSize: 12.5),
                ),
              ),
              if (!_alreadySeated) ...[
                const SizedBox(height: 20),
                const Text('Your buy-in',
                    style:
                        TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
                const SizedBox(height: 12),
                TextField(
                  controller: _buyInController,
                  keyboardType: TextInputType.number,
                  style: const TextStyle(color: KarataColors.ink),
                  decoration: const InputDecoration(labelText: 'Chips'),
                ),
                const SizedBox(height: 14),
                const Text(
                  'You can only buy in once per table, so pick a stack you\'re happy to sit with.',
                  style: TextStyle(fontSize: 12, color: KarataColors.dim, height: 1.5),
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
                    : Text(_alreadySeated ? 'Open table' : 'Sit down'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
