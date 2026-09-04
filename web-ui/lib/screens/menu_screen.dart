import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../theme.dart';

class RecentTable {
  final String gameId;
  final String name;

  RecentTable({required this.gameId, required this.name});

  Map<String, dynamic> toJson() => {'gameId': gameId, 'name': name};
  factory RecentTable.fromJson(Map<String, dynamic> j) =>
      RecentTable(gameId: j['gameId'] as String, name: j['name'] as String);
}

class MenuScreen extends StatefulWidget {
  final String serverUrl;
  final String token;
  final String username;

  const MenuScreen({
    super.key,
    required this.serverUrl,
    required this.token,
    required this.username,
  });

  @override
  State<MenuScreen> createState() => _MenuScreenState();
}

class _MenuScreenState extends State<MenuScreen> {
  List<RecentTable> _recent = [];

  @override
  void initState() {
    super.initState();
    _loadRecent();
  }

  Future<void> _loadRecent() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getStringList('recent_tables') ?? [];
    setState(() {
      _recent = raw
          .map((s) => RecentTable.fromJson(jsonDecode(s) as Map<String, dynamic>))
          .toList();
    });
  }

  Future<void> _logOut() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('jwt_token');
    await prefs.remove('username');
    if (mounted) {
      Navigator.of(context).pushReplacementNamed('/');
    }
  }

  Map<String, dynamic> get _sessionArgs =>
      {'serverUrl': widget.serverUrl, 'token': widget.token, 'username': widget.username};

  void _openTable(String gameId) {
    Navigator.of(context)
        .pushNamed('/table/$gameId', arguments: _sessionArgs)
        .then((_) => _loadRecent());
  }

  Future<void> _createTable() async {
    await Navigator.of(context).pushNamed('/new-table', arguments: _sessionArgs);
    _loadRecent();
  }

  Future<void> _joinTable() async {
    await Navigator.of(context).pushNamed('/join-table', arguments: _sessionArgs);
    _loadRecent();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        actions: [
          IconButton(icon: const Icon(Icons.logout), onPressed: _logOut, tooltip: 'Log out'),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const CircleAvatar(
                    radius: 28,
                    backgroundColor: KarataColors.pill,
                    child: Icon(Icons.person, color: KarataColors.ink),
                  ),
                  const SizedBox(width: 14),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.username,
                        style: const TextStyle(
                            fontSize: 20, fontWeight: FontWeight.w500, color: KarataColors.ink),
                      ),
                      const Text('This name is your sign-in',
                          style: TextStyle(fontSize: 12.5, color: KarataColors.dim)),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 28),
              ElevatedButton.icon(
                onPressed: _createTable,
                icon: const Text('♠', style: TextStyle(color: KarataColors.dim)),
                label: const Text('Create a table'),
              ),
              const SizedBox(height: 11),
              OutlinedButton.icon(
                onPressed: _joinTable,
                icon: const Icon(Icons.subdirectory_arrow_right, size: 18),
                label: const Text('Join with a link'),
              ),
              const SizedBox(height: 32),
              Row(
                children: [
                  const Text('Your tables',
                      style: TextStyle(
                          fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
                  const SizedBox(width: 8),
                  const Text('kept on this phone',
                      style: TextStyle(fontSize: 12.5, color: KarataColors.dim)),
                ],
              ),
              const SizedBox(height: 4),
              Expanded(
                child: _recent.isEmpty
                    ? const Center(
                        child: Text(
                          'No tables yet. Create or join one to see it here.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: KarataColors.dim),
                        ),
                      )
                    : ListView.separated(
                        itemCount: _recent.length,
                        separatorBuilder: (context, index) =>
                            const Divider(height: 1, color: Color(0xFF1A181E)),
                        itemBuilder: (context, index) {
                          final t = _recent[index];
                          return ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: const Icon(Icons.circle, size: 7, color: KarataColors.live),
                            title: Text(t.name,
                                style: const TextStyle(color: KarataColors.ink, fontSize: 16.5)),
                            trailing: TextButton(
                              onPressed: () => _openTable(t.gameId),
                              child: const Text('Open'),
                            ),
                            onTap: () => _openTable(t.gameId),
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

Future<void> saveRecentTable(String gameId, String name) async {
  final prefs = await SharedPreferences.getInstance();
  final raw = prefs.getStringList('recent_tables') ?? [];
  final list = raw
      .map((s) => RecentTable.fromJson(jsonDecode(s) as Map<String, dynamic>))
      .where((t) => t.gameId != gameId)
      .toList();
  list.insert(0, RecentTable(gameId: gameId, name: name));
  await prefs.setStringList(
      'recent_tables', list.map((t) => jsonEncode(t.toJson())).toList());
}
