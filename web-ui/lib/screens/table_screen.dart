import 'dart:async';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
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

  // Purely cosmetic bookkeeping for the turn countdown's progress bar - the deadline itself
  // (_turnDeadline) is always the server's authoritative value; this just remembers when we
  // first observed the current turn so the bar has a start point to animate from.
  String? _turnWindowKey;
  DateTime? _turnWindowStart;

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
      _showError((t) => t.couldNotStartHand('$e'));
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
      _showError((t) => t.actionFailed(actionType, '$e'));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showError(String Function(AppLocalizations) message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
          content: Text(message(AppLocalizations.of(context))), backgroundColor: KarataColors.red),
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
      SnackBar(content: Text(AppLocalizations.of(context).inviteCopied)),
    );
  }

  Future<void> _closeTable() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) {
        final t = AppLocalizations.of(context);
        return AlertDialog(
          title: Text(t.closeTableTitle),
          content: Text(t.closeTableContent),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: Text(t.cancel),
            ),
            TextButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: Text(t.closeTable, style: const TextStyle(color: KarataColors.red)),
            ),
          ],
        );
      },
    );
    if (confirmed != true) return;

    setState(() => _isLoading = true);
    try {
      await _apiClient.closeTable(widget.gameId);
      await _refresh();
    } catch (e) {
      _showError((t) => t.couldNotCloseTable('$e'));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _leaveTable() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) {
        final t = AppLocalizations.of(context);
        return AlertDialog(
          title: Text(t.leaveTableTitle),
          content: Text(t.leaveTableContent),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: Text(t.cancel),
            ),
            TextButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: Text(t.leaveTable, style: const TextStyle(color: KarataColors.red)),
            ),
          ],
        );
      },
    );
    if (confirmed != true) return;

    setState(() => _isLoading = true);
    try {
      await _apiClient.leaveTable(widget.gameId);
      if (!mounted) return;
      // Clears the whole stack rather than just replacing this screen - normally reached via
      // Menu pushing this table on top of itself, so a plain replace would leave that earlier
      // Menu instance underneath and show as a stray back button.
      Navigator.of(context).pushNamedAndRemoveUntil(
        '/menu',
        (route) => false,
        arguments: {
          'serverUrl': widget.serverUrl,
          'token': widget.token,
          'username': widget.username,
        },
      );
    } catch (e) {
      _showError((t) => t.couldNotLeaveTable('$e'));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _toggleSizer(String type) {
    if (_selectedActionType == type) {
      setState(() => _selectedActionType = null);
      return;
    }
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

    final t = AppLocalizations.of(context);
    final gameName = _game?['name'] as String? ?? '';
    final pot = _currentDeal?['pot']?.toString() ?? '0';
    final communityCards =
        _currentDeal?['communityCards'] as List<dynamic>? ?? [null, null, null, null, null];
    final outcome = _currentDeal?['outcome'] as Map<String, dynamic>?;
    final revealedHands = <String, Map<String, dynamic>>{
      for (final rh in (outcome?['revealedHands'] as List<dynamic>? ?? []))
        (rh as Map<String, dynamic>)['playerId'].toString(): rh,
    };

    return Scaffold(
      backgroundColor: KarataColors.bg,
      appBar: AppBar(
        title: Text(_isClosed ? '$gameName (${t.closedSuffix})' : gameName,
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500, color: KarataColors.dim)),
        actions: [
          IconButton(
            icon: const Icon(Icons.ios_share, size: 20),
            tooltip: t.copyInvite,
            onPressed: _copyInvite,
          ),
          if (!_isClosed)
            PopupMenuButton<void>(
              icon: const Icon(Icons.more_vert, size: 20),
              itemBuilder: (context) => [
                PopupMenuItem(
                  onTap: _leaveTable,
                  child: Text(t.leaveTable),
                ),
                PopupMenuItem(
                  onTap: _closeTable,
                  child: Text(t.closeTable, style: const TextStyle(color: KarataColors.red)),
                ),
              ],
            ),
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Center(
              child: Row(
                children: [
                  Icon(Icons.circle,
                      size: 6, color: _isStale ? KarataColors.stale : KarataColors.live),
                  const SizedBox(width: 7),
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
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: _players
                      .map((p) => _SeatWidget(
                            player: p as Map<String, dynamic>,
                            activePlayerId: _activePlayerId,
                            revealedHand: revealedHands[p['playerId']?.toString()],
                          ))
                      .toList(),
                ),
                Expanded(
                  child: Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        _BoardRow(cards: communityCards),
                        const SizedBox(height: 12),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(t.pot,
                                style: const TextStyle(fontSize: 11.5, color: KarataColors.dim)),
                            Text(pot,
                                style: const TextStyle(
                                    fontSize: 22, color: KarataColors.ink, fontWeight: FontWeight.w400)),
                          ],
                        ),
                        if (outcome != null) ...[
                          const SizedBox(height: 14),
                          _OutcomeBanner(outcome: outcome),
                        ],
                      ],
                    ),
                  ),
                ),
                SizedBox(height: 18, child: _buildTurnLine()),
                const SizedBox(height: 10),
                if (_selectedActionType != null) ...[
                  _buildSizer(),
                  const SizedBox(height: 11),
                ],
                _buildActionArea(),
                const SizedBox(height: 22),
                _buildHandRow(),
              ],
            ),
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
    final t = AppLocalizations.of(context);
    if (!_isStale) return t.live;
    final last = _lastUpdated;
    if (last == null) return t.reconnecting;
    final secs = DateTime.now().difference(last).inSeconds;
    return t.lastUpdateAgo(secs);
  }

  Widget _buildTurnLine() {
    if (_isClosed || _dealId.isEmpty || _phase == 'SHOWDOWN') return const SizedBox();
    final t = AppLocalizations.of(context);

    if (_isMyTurn && _turnDeadline != null) {
      final deadline = _turnDeadline!;
      final key = '$_dealId:$_phase:$_activePlayerId';
      if (_turnWindowKey != key) {
        _turnWindowKey = key;
        _turnWindowStart = DateTime.now();
      }
      final remaining = deadline.difference(DateTime.now()).inSeconds;
      final secs = remaining > 0 ? remaining : 0;
      final totalMs = deadline.difference(_turnWindowStart!).inMilliseconds;
      final elapsedMs = DateTime.now().difference(_turnWindowStart!).inMilliseconds;
      final fraction = totalMs > 0 ? (elapsedMs / totalMs).clamp(0.0, 1.0) : 1.0;

      return Row(
        children: [
          _ClockBar(fraction: fraction),
          const SizedBox(width: 8),
          Text(t.yourTurnLeft(secs), style: const TextStyle(fontSize: 13, color: KarataColors.dim)),
        ],
      );
    }

    final active = _players.firstWhere(
      (p) => p['playerId']?.toString() == _activePlayerId,
      orElse: () => null,
    );
    if (active == null) return const SizedBox();
    return Text(t.waitingOn(active['username']?.toString() ?? ''),
        style: const TextStyle(fontSize: 13, color: KarataColors.dim));
  }

  Widget _buildActionArea() {
    final t = AppLocalizations.of(context);
    if (_isClosed) {
      return SizedBox(
        width: double.infinity,
        height: 48,
        child: Center(
          child: Text(t.tableClosed,
              style: const TextStyle(
                  color: KarataColors.dim,
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                  letterSpacing: 1.1)),
        ),
      );
    }

    if (_dealId.isEmpty) {
      return SizedBox(
        width: double.infinity,
        child: ElevatedButton(onPressed: _startHand, child: Text(t.startTheHand)),
      );
    }

    if (_phase == 'SHOWDOWN') {
      return SizedBox(
        width: double.infinity,
        child: ElevatedButton(onPressed: _startHand, child: Text(t.nextHand)),
      );
    }

    final mine = _isMyTurn;
    final you = _you;
    final callAmount = (you?['callAmount'] as num?)?.toInt() ?? 0;
    final minRaise = (you?['minRaise'] as num?)?.toInt() ?? 20;
    final currentRoundBet = (_currentDeal?['currentRoundBet'] as num?)?.toInt() ?? 0;
    final raiseType = currentRoundBet == 0 ? 'BET' : 'RAISE';
    final sizerOpen = _selectedActionType == raiseType;
    final raiseAmount = sizerOpen ? _sizerAmount : minRaise;
    final callLabel = callAmount == 0 ? t.check : t.call(callAmount);
    final raiseLabel = currentRoundBet == 0 ? t.bet(raiseAmount) : t.raise(raiseAmount);

    return Row(
      children: [
        Expanded(
          child: _ActBtn(
            label: t.fold,
            enabled: mine,
            onPressed: () => _submitAction('FOLD'),
          ),
        ),
        const SizedBox(width: 9),
        Expanded(
          child: _ActBtn(
            label: callLabel,
            enabled: mine,
            solid: mine,
            onPressed: () =>
                callAmount == 0 ? _submitAction('CHECK') : _submitAction('CALL', amount: callAmount),
          ),
        ),
        const SizedBox(width: 9),
        Expanded(
          child: _ActBtn(
            label: raiseLabel,
            enabled: mine,
            onPressed: () => sizerOpen
                ? _submitAction(raiseType, amount: _sizerAmount)
                : _submitAction(raiseType, amount: minRaise),
          ),
        ),
        const SizedBox(width: 9),
        _BumpBtn(
          on: sizerOpen,
          enabled: mine,
          onPressed: () => _toggleSizer(raiseType),
        ),
      ],
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

    int clampAmount(int v) => v.clamp(minRaise, maxRaise);
    final t = AppLocalizations.of(context);

    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: KarataColors.pillLine),
        borderRadius: BorderRadius.circular(22),
      ),
      padding: const EdgeInsets.fromLTRB(18, 16, 18, 18),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text('$_sizerAmount',
                  style: const TextStyle(
                      fontSize: 32, fontWeight: FontWeight.w300, color: KarataColors.ink)),
              Text(t.minAllIn(minRaise, maxRaise),
                  style: const TextStyle(fontSize: 11.5, color: KarataColors.dim)),
            ],
          ),
          const SizedBox(height: 16),
          SliderTheme(
            data: SliderTheme.of(context).copyWith(
              trackHeight: 3,
              activeTrackColor: KarataColors.ink,
              inactiveTrackColor: const Color(0xFF26242B),
              thumbColor: KarataColors.ink,
              thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 10),
              overlayShape: SliderComponentShape.noOverlay,
            ),
            child: Slider(
              value: clampAmount(_sizerAmount).toDouble(),
              min: minRaise.toDouble(),
              max: maxRaise > minRaise ? maxRaise.toDouble() : minRaise.toDouble() + 1,
              onChanged: (v) => setState(() => _sizerAmount = v.round()),
            ),
          ),
          Row(
            children: [
              _quickBtn(t.min, _sizerAmount == minRaise, () => setState(() => _sizerAmount = minRaise)),
              _quickBtn(
                  t.halfPot, false, () => setState(() => _sizerAmount = clampAmount(pot ~/ 2))),
              _quickBtn(t.pot, false, () => setState(() => _sizerAmount = clampAmount(pot))),
              _quickBtn(t.allIn, _sizerAmount == maxRaise,
                  () => setState(() => _sizerAmount = maxRaise)),
            ],
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
            side: const BorderSide(color: KarataColors.pillLine),
            backgroundColor: on ? KarataColors.pill : null,
            foregroundColor: on ? KarataColors.ink : KarataColors.dim,
          ),
          child: Text(label, style: const TextStyle(fontSize: 12.5)),
        ),
      ),
    );
  }

  Widget _buildHandRow() {
    return SizedBox(
      height: 132,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
            child: _myCards.isEmpty
                ? Row(children: const [_MutedHoleCard(), SizedBox(width: 0), _MutedHoleCard()])
                : Stack(
                    children: [
                      for (var i = 0; i < _myCards.length; i++)
                        Positioned(
                          left: i * 76.0,
                          child: PokerCardWidget(
                            cardCode: _myCards[i]?.toString(),
                            width: 92,
                            height: 124,
                            rankFontSize: 34,
                            suitFontSize: 26,
                          ),
                        ),
                    ],
                  ),
          ),
          const SizedBox(width: 12),
          const _MadeHandBox(),
        ],
      ),
    );
  }
}

/// The API returns lastAction as a pre-formatted English string (e.g. "CALL 20", "SMALL BLIND
/// 10") derived from the domain action itself, not a translation key - re-parse it here rather
/// than changing the API contract just for client-side display purposes.
String _formatLastAction(AppLocalizations t, String raw) {
  final parts = raw.split(' ');
  if (parts.isEmpty) return raw;
  final amount = int.tryParse(parts.last) ?? 0;
  switch (parts.first) {
    case 'FOLD':
      return t.fold;
    case 'CHECK':
      return t.check;
    case 'CALL':
      return t.call(amount);
    case 'BET':
      return t.bet(amount);
    case 'RAISE':
      return t.raise(amount);
    case 'SMALL':
      return '${t.sb} $amount';
    case 'BIG':
      return '${t.bb} $amount';
    default:
      return raw;
  }
}

class _SeatWidget extends StatelessWidget {
  final Map<String, dynamic> player;
  final String? activePlayerId;
  final Map<String, dynamic>? revealedHand;

  const _SeatWidget({required this.player, required this.activePlayerId, this.revealedHand});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final playerId = player['playerId']?.toString();
    final username = player['username']?.toString() ?? 'Player';
    final chips = player['chips']?.toString() ?? '0';
    final status = player['status']?.toString() ?? 'ACTIVE';
    final blind = player['blind']?.toString();
    final lastActionRaw = player['lastAction']?.toString();
    final lastAction = lastActionRaw != null ? _formatLastAction(t, lastActionRaw) : null;
    final contribution = (player['contributionThisRound'] as num?)?.toInt() ?? 0;
    final isActive = activePlayerId != null && activePlayerId == playerId;
    final isFolded = status == 'FOLDED';
    final isAllIn = status == 'ALL_IN';
    final initial = username.isNotEmpty ? username[0].toUpperCase() : '?';
    final holeCards = revealedHand?['holeCards'] as List<dynamic>?;
    final handRank = revealedHand?['handRank'] as String?;

    return Opacity(
      opacity: isFolded ? 0.26 : 1,
      child: SizedBox(
        width: 64,
        child: Column(
          children: [
            if (lastAction != null)
              // Keying on the action text itself restarts this tween from scratch every time it
              // changes to a genuinely new value (including its first appearance), giving a brief
              // "just happened" pop without any manual AnimationController bookkeeping.
              TweenAnimationBuilder<double>(
                key: ValueKey(lastActionRaw), // raw, not the localized text - a language switch
                // shouldn't replay this pop, only a genuinely new action should.
                tween: Tween(begin: 1.4, end: 1.0),
                duration: const Duration(milliseconds: 380),
                curve: Curves.easeOutBack,
                builder: (context, scale, child) => Transform.scale(scale: scale, child: child),
                child: Container(
                  margin: const EdgeInsets.only(bottom: 5),
                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                  decoration: BoxDecoration(
                    color: KarataColors.live,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(lastAction,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                          fontSize: 9.5,
                          fontWeight: FontWeight.w700,
                          color: KarataColors.cardInk,
                          height: 1)),
                ),
              ),
            SizedBox(
              width: 64,
              height: 46,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  Center(
                    child: Container(
                      width: 46,
                      height: 46,
                      decoration: BoxDecoration(
                        color: const Color(0xFF221F28),
                        shape: BoxShape.circle,
                        border: isActive ? Border.all(color: KarataColors.ink, width: 2) : null,
                      ),
                      alignment: Alignment.center,
                      child: Text(initial,
                          style: const TextStyle(
                              fontSize: 18, color: KarataColors.ink, fontWeight: FontWeight.w500)),
                    ),
                  ),
                  if (blind != null || isAllIn)
                    Positioned(
                      top: 0,
                      right: -1,
                      child: Container(
                        height: 16,
                        padding: const EdgeInsets.symmetric(horizontal: 5),
                        decoration: BoxDecoration(
                          color: isAllIn ? KarataColors.allInBg : const Color(0xFF2E2C34),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        alignment: Alignment.center,
                        child: Text(
                          isAllIn ? t.allInTag : (blind == 'SMALL' ? t.sb : t.bb),
                          style: TextStyle(
                              fontSize: 9,
                              fontWeight: FontWeight.w600,
                              color: isAllIn ? KarataColors.allInInk : KarataColors.ink),
                        ),
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 7),
            Text(username,
                textAlign: TextAlign.center,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 11, color: KarataColors.dim)),
            Text(chips,
                style: const TextStyle(
                    fontSize: 15, color: KarataColors.ink, fontWeight: FontWeight.w500)),
            if (contribution > 0)
              Container(
                margin: const EdgeInsets.only(top: 8),
                height: 22,
                padding: const EdgeInsets.symmetric(horizontal: 7),
                decoration: BoxDecoration(
                  color: KarataColors.chipBg,
                  borderRadius: BorderRadius.circular(11),
                ),
                alignment: Alignment.center,
                child: Text('$contribution',
                    style: const TextStyle(
                        color: KarataColors.chipInk, fontSize: 10.5, fontWeight: FontWeight.w600)),
              ),
            // Cards flip face-up at each seat that reached a real showdown (won or lost) once
            // the hand concludes - a folded player has no entry here and stays hidden, per usual
            // poker etiquette.
            if (holeCards != null && holeCards.length == 2) ...[
              const SizedBox(height: 8),
              SizedBox(
                width: 64,
                height: 42,
                child: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    Positioned(
                      left: 3,
                      child: PokerCardWidget(
                        cardCode: holeCards[0]?.toString(),
                        width: 30,
                        height: 42,
                        rankFontSize: 12,
                        suitFontSize: 10,
                      ),
                    ),
                    Positioned(
                      left: 27,
                      child: PokerCardWidget(
                        cardCode: holeCards[1]?.toString(),
                        width: 30,
                        height: 42,
                        rankFontSize: 12,
                        suitFontSize: 10,
                      ),
                    ),
                  ],
                ),
              ),
              if (handRank != null)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(handRank,
                      textAlign: TextAlign.center,
                      overflow: TextOverflow.ellipsis,
                      maxLines: 1,
                      style: const TextStyle(fontSize: 8.5, color: KarataColors.dim)),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

class _BoardRow extends StatelessWidget {
  final List<dynamic> cards;
  const _BoardRow({required this.cards});

  @override
  Widget build(BuildContext context) {
    const cardWidth = 64.0;
    const step = 58.0; // cardWidth - 6px overlap, matching the mockup
    final width = cardWidth + step * (cards.length - 1);
    return SizedBox(
      width: width,
      height: 86,
      child: Stack(
        children: [
          for (var i = 0; i < cards.length; i++)
            Positioned(
              left: i * step,
              child: PokerCardWidget(cardCode: cards[i]?.toString()),
            ),
        ],
      ),
    );
  }
}

class _ActBtn extends StatelessWidget {
  final String label;
  final bool enabled;
  final bool solid;
  final VoidCallback onPressed;

  const _ActBtn({
    required this.label,
    required this.enabled,
    required this.onPressed,
    this.solid = false,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 48,
      child: OutlinedButton(
        onPressed: enabled ? onPressed : null,
        style: OutlinedButton.styleFrom(
          backgroundColor: solid ? KarataColors.pill : Colors.transparent,
          side: BorderSide(color: solid ? Colors.transparent : KarataColors.pillLine),
          foregroundColor: KarataColors.ink,
          disabledForegroundColor: KarataColors.ink.withValues(alpha: 0.3),
          disabledBackgroundColor: solid ? KarataColors.pill.withValues(alpha: 0.3) : null,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
        ),
        child: Text(label,
            style: const TextStyle(fontSize: 14.5), overflow: TextOverflow.ellipsis, maxLines: 1),
      ),
    );
  }
}

class _BumpBtn extends StatelessWidget {
  final bool on;
  final bool enabled;
  final VoidCallback onPressed;

  const _BumpBtn({required this.on, required this.enabled, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 48,
      height: 48,
      child: OutlinedButton(
        onPressed: enabled ? onPressed : null,
        style: OutlinedButton.styleFrom(
          padding: EdgeInsets.zero,
          backgroundColor: on ? KarataColors.pill : Colors.transparent,
          side: BorderSide(color: on ? Colors.transparent : KarataColors.pillLine),
          foregroundColor: KarataColors.ink,
          disabledForegroundColor: KarataColors.ink.withValues(alpha: 0.3),
          shape: const CircleBorder(),
        ),
        child: const Icon(Icons.arrow_upward, size: 16),
      ),
    );
  }
}

class _ClockBar extends StatelessWidget {
  final double fraction;
  const _ClockBar({required this.fraction});

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(2),
      child: SizedBox(
        width: 30,
        height: 3,
        child: Stack(
          children: [
            Container(color: const Color(0xFF26242B)),
            FractionallySizedBox(
              widthFactor: 1 - fraction,
              child: Container(color: KarataColors.ink),
            ),
          ],
        ),
      ),
    );
  }
}

class _MadeHandBox extends StatelessWidget {
  const _MadeHandBox();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 150,
      height: 124,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        border: Border.all(color: KarataColors.pillLine),
        borderRadius: BorderRadius.circular(15),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 10),
      child: Text(AppLocalizations.of(context).handStrengthAvailableSoon,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 12.5, color: KarataColors.dim)),
    );
  }
}

class _MutedHoleCard extends StatelessWidget {
  const _MutedHoleCard();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 92,
      height: 124,
      margin: const EdgeInsets.only(right: 8),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFF201E25)),
        borderRadius: BorderRadius.circular(15),
      ),
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
    return Wrap(
      alignment: WrapAlignment.end,
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: 9,
      children: [
        Text(AppLocalizations.of(context).won(names, total),
            style: const TextStyle(
                color: KarataColors.ink, fontSize: 14.5, fontWeight: FontWeight.w500)),
        if (rank != null)
          Text(rank, style: const TextStyle(color: KarataColors.dim, fontSize: 12.5)),
      ],
    );
  }
}

class PokerCardWidget extends StatelessWidget {
  final String? cardCode;
  final double width;
  final double height;
  final double rankFontSize;
  final double suitFontSize;

  const PokerCardWidget({
    super.key,
    this.cardCode,
    this.width = 64,
    this.height = 86,
    this.rankFontSize = 24,
    this.suitFontSize = 19,
  });

  @override
  Widget build(BuildContext context) {
    final radius = BorderRadius.circular(width > 80 ? 15 : 11);
    if (cardCode == null) {
      // A card slot that exists but hasn't been revealed yet - a face-down card, not an empty
      // one, since the API always sends a fixed-size community-card array padded with nulls.
      return Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: KarataColors.card,
          borderRadius: radius,
          border: Border.all(color: KarataColors.bg, width: 2),
        ),
        child: ClipRRect(borderRadius: radius, child: CustomPaint(painter: _CardBackPainter())),
      );
    }

    final code = cardCode!;
    if (code.length < 2) return const SizedBox();
    final suitChar = code[code.length - 1].toLowerCase();
    final rank = code.substring(0, code.length - 1).toUpperCase();

    const suitSymbols = {'c': '♣', 'd': '♦', 'h': '♥', 's': '♠'};
    const redSuits = {'d', 'h'};
    final suitSymbol = suitSymbols[suitChar] ?? '?';
    final suitColor = redSuits.contains(suitChar) ? KarataColors.red : KarataColors.cardInk;

    return Container(
      width: width,
      height: height,
      padding: const EdgeInsets.only(top: 7, left: 8),
      decoration: BoxDecoration(
        color: KarataColors.card,
        borderRadius: radius,
        border: Border.all(color: KarataColors.bg, width: 2),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(rank,
              style: TextStyle(
                  color: suitColor,
                  fontSize: rank.length > 1 ? rankFontSize * 0.8 : rankFontSize,
                  height: 1,
                  fontWeight: FontWeight.w500)),
          Text(suitSymbol, style: TextStyle(color: suitColor, fontSize: suitFontSize, height: 1)),
        ],
      ),
    );
  }
}

class _CardBackPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFFCBC9C5)
      ..strokeWidth = 4;
    const gap = 9.0;
    final diag = size.width + size.height;
    for (double x = -size.height; x < diag; x += gap) {
      canvas.drawLine(Offset(x, 0), Offset(x + size.height, size.height), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
