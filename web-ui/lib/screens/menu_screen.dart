import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
import '../locale_controller.dart';
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
  late final ApiClient _apiClient;
  List<RecentTable> _recent = [];

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient(baseUrl: widget.serverUrl, token: widget.token);
    _loadRecent();
  }

  Future<void> _loadRecent() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getStringList('recent_tables') ?? [];
    final all = raw.map((s) => RecentTable.fromJson(jsonDecode(s) as Map<String, dynamic>)).toList();

    // Closed tables are permanent - drop them from the list rather than just hiding them, so we
    // don't keep re-checking a table that can never reopen on every future menu visit.
    final stillOpen = <RecentTable>[];
    for (final t in all) {
      try {
        final game = await _apiClient.getGame(t.gameId);
        if (game['closed'] != true) stillOpen.add(t);
      } catch (_) {
        // Couldn't confirm status (e.g. offline) - keep it rather than risk hiding a live table.
        stillOpen.add(t);
      }
    }

    if (stillOpen.length != all.length) {
      await prefs.setStringList(
          'recent_tables', stillOpen.map((t) => jsonEncode(t.toJson())).toList());
    }

    if (!mounted) return;
    setState(() => _recent = stillOpen);
  }

  Future<void> _logOut() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('jwt_token');
    await prefs.remove('username');
    if (mounted) {
      Navigator.of(context).pushNamedAndRemoveUntil('/', (route) => false);
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
    final t = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(
        actions: [
          PopupMenuButton<Locale?>(
            icon: const Icon(Icons.language),
            tooltip: t.language,
            onSelected: (locale) => LocaleController.instance.setLocale(locale),
            itemBuilder: (context) => [
              PopupMenuItem(value: null, child: Text(t.systemDefault)),
              const PopupMenuItem(value: Locale('en'), child: Text('English')),
              const PopupMenuItem(value: Locale('fr'), child: Text('Français')),
            ],
          ),
          IconButton(icon: const Icon(Icons.logout), onPressed: _logOut, tooltip: t.logOut),
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
                      Text(t.signInHint,
                          style: const TextStyle(fontSize: 12.5, color: KarataColors.dim)),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 28),
              ElevatedButton.icon(
                onPressed: _createTable,
                icon: const Text('♠', style: TextStyle(color: KarataColors.dim)),
                label: Text(t.createTable),
              ),
              const SizedBox(height: 11),
              OutlinedButton.icon(
                onPressed: _joinTable,
                icon: const Icon(Icons.subdirectory_arrow_right, size: 18),
                label: Text(t.joinWithLink),
              ),
              const SizedBox(height: 32),
              Row(
                children: [
                  Text(t.yourTables,
                      style: const TextStyle(
                          fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
                  const SizedBox(width: 8),
                  Text(t.keptOnThisDevice,
                      style: const TextStyle(fontSize: 12.5, color: KarataColors.dim)),
                ],
              ),
              const SizedBox(height: 4),
              Expanded(
                child: _recent.isEmpty
                    ? Center(
                        child: Text(
                          t.noTablesYet,
                          textAlign: TextAlign.center,
                          style: const TextStyle(color: KarataColors.dim),
                        ),
                      )
                    : ListView.separated(
                        itemCount: _recent.length,
                        separatorBuilder: (context, index) =>
                            const Divider(height: 1, color: Color(0xFF1A181E)),
                        itemBuilder: (context, index) {
                          final rt = _recent[index];
                          return ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: const Icon(Icons.circle, size: 7, color: KarataColors.live),
                            title: Text(rt.name,
                                style: const TextStyle(color: KarataColors.ink, fontSize: 16.5)),
                            trailing: TextButton(
                              onPressed: () => _openTable(rt.gameId),
                              child: Text(t.open),
                            ),
                            onTap: () => _openTable(rt.gameId),
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
