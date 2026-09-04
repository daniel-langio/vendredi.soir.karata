import 'dart:async';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../api/api_client.dart';
import '../theme.dart';

class TableScreen extends StatefulWidget {
  final String serverUrl;
  final String token;
  final String username;
  final String gameId;

  const TableScreen({
    super.key,
    required this.serverUrl,
    required this.token,
    required this.username,
    required this.gameId,
  });

  @override
  State<TableScreen> createState() => _TableScreenState();
}

class _TableScreenState extends State<TableScreen> {
  late final ApiClient _apiClient;
  Timer? _pollTimer;
  Timer? _tickTimer;

  Map<String, dynamic>? _game;
  List<dynamic> _myCards = [];
  bool _isLoading = false;
  bool _isStale = false;
  DateTime? _lastUpdated;

  String? _selectedActionType; // 'BET' or 'RAISE' while the sizer is open
  int _sizerAmount = 0;

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient(baseUrl: widget.serverUrl, token: widget.token);
    _refresh(showSpinner: true);
    _pollTimer = Timer.periodic(const Duration(seconds: 2), (_) => _refresh());
    _tickTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _tickTimer?.cancel();
    super.dispose();
  }

  Map<String, dynamic>? get _currentDeal => _game?['currentDeal'] as Map<String, dynamic>?;
  String get _dealId => _game?['currentDealId']?.toString() ?? '';
  String get _phase => _currentDeal?['phase']?.toString() ?? '';
  String? get _activePlayerId => _currentDeal?['activePlayerId']?.toString();
  Map<String, dynamic>? get _you => _game?['you'] as Map<String, dynamic>?;
  List<dynamic> get _players => _game?['players'] as List<dynamic>? ?? [];
  List<dynamic> get _opponents =>
      _players.where((p) => p['username'] != widget.username).toList();
  Map<String, dynamic>? get _me => _players.firstWhere(
        (p) => p['username'] == widget.username,
        orElse: () => null,
      ) as Map<String, dynamic>?;
  bool get _isClosed => _game?['closed'] == true;
  bool get _isMyTurn {
    final me = _players.firstWhere((p) => p['username'] == widget.username, orElse: () => null);
    return me != null && _activePlayerId != null && _activePlayerId == me['playerId']?.toString();
  }

  /// The server's authoritative deadline for whoever's on the clock to act - see
  /// DealService.enforceTurnTimeout on the backend, which auto-folds past this point.
  DateTime? get _turnDeadline {
    final raw = _currentDeal?['turnDeadline'] as String?;
    return raw != null ? DateTime.parse(raw) : null;
  }

  Future<void> _refresh({bool showSpinner = false}) async {
    if (showSpinner && mounted) setState(() => _isLoading = true);
    try {
      final game = await _apiClient.getGame(widget.gameId);
      final dealId = game['currentDealId']?.toString();

      List<dynamic> cards = _myCards;
      if (dealId != null) {
        try {
          final handData = await _apiClient.getMyHand(dealId);
          cards = handData['cards'] as List<dynamic>? ?? [];
        } catch (_) {
          // Hand not accessible yet (e.g. between hands) - keep the last known cards.
        }
      } else {
        cards = [];
      }

      if (!mounted) return;
      setState(() {
        _game = game;
        _myCards = cards;
        _isStale = false;
        _lastUpdated = DateTime.now();
      });
    } catch (_) {
      if (mounted) setState(() => _isStale = true);
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _startHand() async {
    setState(() => _isLoading = true);
    try {
      final game = await _apiClient.startDeal(widget.gameId);
      if (mounted) setState(() => _game = game);
      await _refresh();
    } catch (e) {
      _showError('Could not start hand: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _submitAction(String actionType, {int? amount}) async {
    if (_dealId.isEmpty) return;
    setState(() => _isLoading = true);
    try {
      await _apiClient.takeAction(dealId: _dealId, actionType: actionType, amount: amount);
      setState(() => _selectedActionType = null);
      await _refresh();
    } catch (e) {
      _showError('$actionType failed: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: KarataColors.red),
    );
  }

  Future<void> _copyInvite() async {
    // On web this is a real clickable link (the app has proper per-page paths - see
    // main.dart's routing); a native build has no meaningful "origin" to build one from,
    // so it falls back to the bare table ID, which the join-table screen also accepts.
    final invite = kIsWeb ? '${Uri.base.origin}/table/${widget.gameId}' : widget.gameId;
    await Clipboard.setData(ClipboardData(text: invite));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Invite copied — send it to whoever you want to invite')),
    );
  }

  Future<void> _closeTable() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Close this table?'),
        content: const Text(
          'Nobody will be able to join, start a hand, or act at this table again. '
          'This cannot be undone.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Close table', style: TextStyle(color: KarataColors.red)),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() => _isLoading = true);
    try {
      await _apiClient.closeTable(widget.gameId);
      await _refresh();
    } catch (e) {
      _showError('Could not close table: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _openSizer(String type) {
    final you = _you;
    final minRaise = (you?['minRaise'] as num?)?.toInt() ?? 20;
    setState(() {
      _selectedActionType = type;
      _sizerAmount = minRaise;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_game == null) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    final gameName = _game?['name'] as String? ?? '';
    final pot = _currentDeal?['pot']?.toString() ?? '0';
    final communityCards =
        _currentDeal?['communityCards'] as List<dynamic>? ?? [null, null, null, null, null];
    final outcome = _currentDeal?['outcome'] as Map<String, dynamic>?;

    return Scaffold(
      backgroundColor: KarataColors.bg,
      appBar: AppBar(
        title: Text(_isClosed ? '$gameName (closed)' : gameName),
        actions: [
          IconButton(
            icon: const Icon(Icons.ios_share, size: 20),
            tooltip: 'Copy invite',
            onPressed: _copyInvite,
          ),
          if (!_isClosed)
            PopupMenuButton<void>(
              icon: const Icon(Icons.more_vert, size: 20),
              itemBuilder: (context) => [
                PopupMenuItem(
                  onTap: _closeTable,
                  child: const Text('Close table', style: TextStyle(color: KarataColors.red)),
                ),
              ],
            ),
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Center(
              child: Row(
                children: [
                  Icon(Icons.circle,
                      size: 7, color: _isStale ? KarataColors.stale : KarataColors.live),
                  const SizedBox(width: 6),
                  Text(_liveStatusLabel(),
                      style: const TextStyle(fontSize: 11.5, color: KarataColors.dim)),
                ],
              ),
            ),
          ),
        ],
      ),
      body: Stack(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                flex: 5,
                child: Container(
                  margin: const EdgeInsets.all(8),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFF14210E),
                    borderRadius: BorderRadius.circular(30),
                    border: Border.all(color: const Color(0xFF3E2A1A), width: 8),
                  ),
                  child: Stack(
                    children: [
                      Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text('POT: $pot CHIPS',
                              style: const TextStyle(
                                  color: KarataColors.chipInk,
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  letterSpacing: 1.2)),
                          const SizedBox(height: 6),
                          if (_phase.isNotEmpty)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                              decoration: BoxDecoration(
                                color: Colors.black38,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(_phase,
                                  style: const TextStyle(
                                      color: Colors.white70,
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold)),
                            ),
                          const SizedBox(height: 16),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: communityCards
                                .map((c) => Padding(
                                      padding: const EdgeInsets.symmetric(horizontal: 4),
                                      child: PokerCardWidget(cardCode: c?.toString()),
                                    ))
                                .toList(),
                          ),
                          if (outcome != null) ...[
                            const SizedBox(height: 14),
                            _OutcomeBanner(outcome: outcome),
                          ],
                        ],
                      ),
                      Positioned(
                        top: 0,
                        left: 0,
                        right: 0,
                        child: Column(
                          children: [
                            SizedBox(
                              height: 90,
                              child: ListView.builder(
                                scrollDirection: Axis.horizontal,
                                padding: const EdgeInsets.symmetric(horizontal: 8),
                                itemCount: _opponents.length,
                                itemBuilder: (context, idx) => _SeatCard(
                                  player: _opponents[idx] as Map<String, dynamic>,
                                  activePlayerId: _activePlayerId,
                                  myUsername: widget.username,
                                ),
                              ),
                            ),
                            if (_isMyTurn && outcome == null && !_isClosed)
                              const Padding(
                                padding: EdgeInsets.only(top: 4),
                                child: _YourTurnBadge(),
                              ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              SizedBox(
                height: 95,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  itemCount: _me != null ? 1 : 0,
                  itemBuilder: (context, idx) => _SeatCard(
                    player: _me!,
                    activePlayerId: _activePlayerId,
                    myUsername: widget.username,
                  ),
                ),
              ),
              Container(
                color: const Color(0xFF212121),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                child: Row(
                  children: [
                    const Text('MY HAND:',
                        style: TextStyle(
                            color: KarataColors.ink, fontWeight: FontWeight.bold, fontSize: 13)),
                    const SizedBox(width: 12),
                    if (_myCards.isEmpty)
                      const Text('Waiting for deal...',
                          style: TextStyle(color: KarataColors.dim, fontSize: 13))
                    else
                      ..._myCards.map((c) => Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 4),
                            child: PokerCardWidget(cardCode: c?.toString(), width: 40, height: 56),
                          )),
                    const Spacer(),
                    const _MadeHandBadge(),
                  ],
                ),
              ),
              _buildTurnLine(),
              _buildActionArea(),
            ],
          ),
          if (_isLoading)
            Container(
              color: Colors.black45,
              child: const Center(child: CircularProgressIndicator()),
            ),
        ],
      ),
    );
  }

  String _liveStatusLabel() {
    if (!_isStale) return 'Live';
    final last = _lastUpdated;
    if (last == null) return 'Reconnecting';
    final secs = DateTime.now().difference(last).inSeconds;
    return 'Last update ${secs}s ago';
  }

  Widget _buildTurnLine() {
    if (_isClosed || _dealId.isEmpty || _phase == 'SHOWDOWN') return const SizedBox();

    if (_isMyTurn && _turnDeadline != null) {
      final remaining = _turnDeadline!.difference(DateTime.now()).inSeconds;
      final secs = remaining > 0 ? remaining : 0;
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        child: Row(
          children: [
            const Icon(Icons.timer_outlined, size: 14, color: KarataColors.dim),
            const SizedBox(width: 6),
            Text('Your turn — ${secs}s left',
                style: const TextStyle(fontSize: 13, color: KarataColors.dim)),
          ],
        ),
      );
    }

    final active = _players.firstWhere(
      (p) => p['playerId']?.toString() == _activePlayerId,
      orElse: () => null,
    );
    if (active == null) return const SizedBox();
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Text('Waiting on ${active['username']}',
          style: const TextStyle(fontSize: 13, color: KarataColors.dim)),
    );
  }

  Widget _buildActionArea() {
    if (_isClosed) {
      return _ActionBarContainer(
        child: const Center(
          child: Text('THIS TABLE IS CLOSED',
              style: TextStyle(
                  color: KarataColors.dim,
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                  letterSpacing: 1.1)),
        ),
      );
    }

    if (_dealId.isEmpty) {
      return _ActionBarContainer(
        child: SizedBox(
          width: double.infinity,
          child: ElevatedButton(onPressed: _startHand, child: const Text('Start the hand')),
        ),
      );
    }

    if (_phase == 'SHOWDOWN') {
      return _ActionBarContainer(
        child: SizedBox(
          width: double.infinity,
          child: ElevatedButton(onPressed: _startHand, child: const Text('Next hand')),
        ),
      );
    }

    if (_selectedActionType != null) {
      return _buildSizer();
    }

    if (!_isMyTurn) {
      return _ActionBarContainer(
        child: const Center(
          child: Text('WAITING FOR OTHER PLAYERS...',
              style: TextStyle(
                  color: KarataColors.dim,
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                  letterSpacing: 1.1)),
        ),
      );
    }

    final you = _you;
    final callAmount = (you?['callAmount'] as num?)?.toInt() ?? 0;
    final minRaise = (you?['minRaise'] as num?)?.toInt() ?? 20;
    final currentRoundBet = (_currentDeal?['currentRoundBet'] as num?)?.toInt() ?? 0;
    final callLabel = callAmount == 0 ? 'Check' : 'Call $callAmount';
    final raiseLabel = currentRoundBet == 0 ? 'Bet $minRaise' : 'Raise $minRaise';
    final raiseType = currentRoundBet == 0 ? 'BET' : 'RAISE';

    return _ActionBarContainer(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _actionBtn('Fold', KarataColors.red, () => _submitAction('FOLD')),
          _actionBtn(
            callLabel,
            KarataColors.live,
            () => callAmount == 0
                ? _submitAction('CHECK')
                : _submitAction('CALL', amount: callAmount),
          ),
          _actionBtn(raiseLabel, KarataColors.chipInk, () => _submitAction(raiseType, amount: minRaise)),
          IconButton(
            onPressed: () => _openSizer(raiseType),
            icon: const Icon(Icons.arrow_upward, color: KarataColors.ink),
            style: IconButton.styleFrom(
              backgroundColor: KarataColors.pill,
              shape: const CircleBorder(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _actionBtn(String label, Color color, VoidCallback onPressed) {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 3),
        child: ElevatedButton(
          onPressed: onPressed,
          style: ElevatedButton.styleFrom(
            backgroundColor: KarataColors.pill,
            foregroundColor: color,
            minimumSize: const Size.fromHeight(48),
          ),
          child: Text(label, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
        ),
      ),
    );
  }

  Widget _buildSizer() {
    final you = _you;
    final rawMinRaise = (you?['minRaise'] as num?)?.toInt() ?? 20;
    final rawMaxRaise = (you?['maxRaise'] as num?)?.toInt() ?? rawMinRaise;
    // A short stack can have fewer chips than the table's minimum raise (e.g. an all-in for
    // less); guard against that so bounds never invert (min > max would crash Slider/clamp).
    final minRaise = rawMinRaise <= rawMaxRaise ? rawMinRaise : rawMaxRaise;
    final maxRaise = rawMaxRaise;
    final pot = (_currentDeal?['pot'] as num?)?.toInt() ?? 0;
    final type = _selectedActionType!;

    int clampAmount(int v) => v.clamp(minRaise, maxRaise);

    return _ActionBarContainer(
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('$type AMOUNT: $_sizerAmount',
                  style: const TextStyle(
                      color: KarataColors.chipInk, fontWeight: FontWeight.bold, fontSize: 14)),
              TextButton(
                onPressed: () => setState(() => _selectedActionType = null),
                child: const Text('Back', style: TextStyle(color: KarataColors.red)),
              ),
            ],
          ),
          Slider(
            value: clampAmount(_sizerAmount).toDouble(),
            min: minRaise.toDouble(),
            max: maxRaise > minRaise ? maxRaise.toDouble() : minRaise.toDouble() + 1,
            activeColor: KarataColors.chipInk,
            onChanged: (v) => setState(() => _sizerAmount = v.round()),
          ),
          Row(
            children: [
              _quickBtn('Min', _sizerAmount == minRaise, () => setState(() => _sizerAmount = minRaise)),
              _quickBtn('½ pot', false, () => setState(() => _sizerAmount = clampAmount(pot ~/ 2))),
              _quickBtn('Pot', false, () => setState(() => _sizerAmount = clampAmount(pot))),
              _quickBtn(
                  'All in', _sizerAmount == maxRaise, () => setState(() => _sizerAmount = maxRaise)),
            ],
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () => _submitAction(type, amount: _sizerAmount),
              style: ElevatedButton.styleFrom(
                  backgroundColor: KarataColors.chipInk, foregroundColor: Colors.black),
              child: Text('CONFIRM $type'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _quickBtn(String label, bool on, VoidCallback onPressed) {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 3),
        child: OutlinedButton(
          onPressed: onPressed,
          style: OutlinedButton.styleFrom(
            minimumSize: const Size.fromHeight(34),
            backgroundColor: on ? KarataColors.pill : null,
            foregroundColor: on ? KarataColors.ink : KarataColors.dim,
          ),
          child: Text(label, style: const TextStyle(fontSize: 12.5)),
        ),
      ),
    );
  }
}

class _ActionBarContainer extends StatelessWidget {
  final Widget child;
  const _ActionBarContainer({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF121212),
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 10),
      child: child,
    );
  }
}

class _YourTurnBadge extends StatelessWidget {
  const _YourTurnBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 4),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFB8860B),
        borderRadius: BorderRadius.circular(20),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.star, size: 16, color: Colors.white),
          SizedBox(width: 4),
          Text('YOUR TURN TO ACT!',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
        ],
      ),
    );
  }
}

class _MadeHandBadge extends StatelessWidget {
  const _MadeHandBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 130,
      height: 56,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        border: Border.all(color: KarataColors.pillLine),
        borderRadius: BorderRadius.circular(12),
      ),
      child: const Text('Hand strength\navailable soon',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 11, color: KarataColors.dim)),
    );
  }
}

class _OutcomeBanner extends StatelessWidget {
  final Map<String, dynamic> outcome;
  const _OutcomeBanner({required this.outcome});

  @override
  Widget build(BuildContext context) {
    final winners = outcome['winners'] as List<dynamic>? ?? [];
    if (winners.isEmpty) return const SizedBox();
    final names = winners.map((w) => w['username']).join(' & ');
    final total = winners.fold<int>(0, (sum, w) => sum + ((w['amount'] as num?)?.toInt() ?? 0));
    final rank = (winners.first as Map<String, dynamic>)['handRank'] as String?;
    return Column(
      children: [
        Text('💰 $names won $total',
            style: const TextStyle(
                color: KarataColors.ink, fontSize: 14.5, fontWeight: FontWeight.w500)),
        if (rank != null)
          Text(rank, style: const TextStyle(color: KarataColors.dim, fontSize: 12.5)),
      ],
    );
  }
}

class _SeatCard extends StatelessWidget {
  final Map<String, dynamic> player;
  final String? activePlayerId;
  final String myUsername;

  const _SeatCard({required this.player, required this.activePlayerId, required this.myUsername});

  @override
  Widget build(BuildContext context) {
    final playerId = player['playerId']?.toString();
    final username = player['username']?.toString() ?? 'Player';
    final chips = player['chips']?.toString() ?? '0';
    final status = player['status']?.toString() ?? 'ACTIVE';
    final blind = player['blind']?.toString();
    final lastAction = player['lastAction']?.toString();
    final contribution = (player['contributionThisRound'] as num?)?.toInt() ?? 0;
    final isActive = activePlayerId != null && activePlayerId == playerId;
    final isMe = username == myUsername;
    final isFolded = status == 'FOLDED';
    final isAllIn = status == 'ALL_IN';

    return Opacity(
      opacity: isFolded ? 0.35 : 1,
      child: Container(
        width: 112,
        margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
        padding: const EdgeInsets.symmetric(vertical: 6),
        decoration: BoxDecoration(
          color: isMe ? const Color(0xFF2A1A40) : const Color(0xFF221F28),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isActive ? const Color(0xFFB8860B) : Colors.transparent,
            width: isActive ? 2.5 : 1,
          ),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (lastAction != null)
              Container(
                margin: const EdgeInsets.only(bottom: 3),
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                decoration: BoxDecoration(
                  color: const Color(0xFF33313A),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(lastAction,
                    style: const TextStyle(
                        fontSize: 8, fontWeight: FontWeight.bold, color: KarataColors.dim)),
              ),
            Stack(
              alignment: Alignment.center,
              children: [
                Text(isMe ? '$username (You)' : username,
                    textAlign: TextAlign.center,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                        color: KarataColors.ink,
                        fontWeight: isMe ? FontWeight.bold : FontWeight.normal,
                        fontSize: 11)),
              ],
            ),
            const SizedBox(height: 4),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.circle, size: 9, color: KarataColors.chipInk),
                const SizedBox(width: 3),
                Text(chips,
                    style: const TextStyle(
                        color: Colors.white70, fontSize: 12, fontWeight: FontWeight.bold)),
              ],
            ),
            if (blind != null || isAllIn)
              Padding(
                padding: const EdgeInsets.only(top: 3),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                  decoration: BoxDecoration(
                    color: isAllIn ? KarataColors.allInBg : const Color(0xFF2E2C34),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    isAllIn ? 'ALL IN' : (blind == 'SMALL' ? 'SB' : 'BB'),
                    style: TextStyle(
                        fontSize: 8,
                        fontWeight: FontWeight.bold,
                        color: isAllIn ? KarataColors.allInInk : KarataColors.ink),
                  ),
                ),
              ),
            if (contribution > 0)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 1),
                  decoration: BoxDecoration(
                    color: KarataColors.chipBg,
                    borderRadius: BorderRadius.circular(11),
                  ),
                  child: Text('$contribution',
                      style: const TextStyle(
                          color: KarataColors.chipInk, fontSize: 10.5, fontWeight: FontWeight.bold)),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class PokerCardWidget extends StatelessWidget {
  final String? cardCode;
  final double width;
  final double height;

  const PokerCardWidget({super.key, this.cardCode, this.width = 50, this.height = 70});

  @override
  Widget build(BuildContext context) {
    if (cardCode == null) {
      return Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: const Color(0xFF8B1E2E),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: KarataColors.ink, width: 2),
        ),
        child: const Center(child: Icon(Icons.help_outline, color: KarataColors.ink, size: 20)),
      );
    }

    final code = cardCode!;
    if (code.length < 2) return const SizedBox();
    final suitChar = code[code.length - 1].toLowerCase();
    final rank = code.substring(0, code.length - 1).toUpperCase();

    const suitSymbols = {'c': '♣', 'd': '♦', 'h': '♥', 's': '♠'};
    const redSuits = {'d', 'h'};
    final suitSymbol = suitSymbols[suitChar] ?? '?';
    final suitColor = redSuits.contains(suitChar) ? KarataColors.red : Colors.black;

    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: KarataColors.card,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: Colors.grey.shade400),
      ),
      child: Stack(
        children: [
          Positioned(
            top: 4,
            left: 4,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(rank,
                    style: TextStyle(color: suitColor, fontSize: 14, fontWeight: FontWeight.bold)),
                Text(suitSymbol, style: TextStyle(color: suitColor, fontSize: 12)),
              ],
            ),
          ),
          Center(
            child: Text(suitSymbol,
                style: TextStyle(color: suitColor.withValues(alpha: 0.15), fontSize: 28)),
          ),
        ],
      ),
    );
  }
}
