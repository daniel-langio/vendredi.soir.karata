import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiClient {
  final String baseUrl;
  final String token;

  ApiClient({required this.baseUrl, this.token = ''});

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        if (token.isNotEmpty) 'Authorization': 'Bearer $token',
      };

  /// POST /auth/register
  Future<String> register(String username, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/register'),
      headers: _headers,
      body: jsonEncode({'username': username, 'password': password}),
    );
    if (response.statusCode == 201) {
      return (jsonDecode(response.body) as Map<String, dynamic>)['token'] as String;
    }
    _throwDetailedError(response);
  }

  /// POST /auth/login
  Future<String> login(String username, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/login'),
      headers: _headers,
      body: jsonEncode({'username': username, 'password': password}),
    );
    if (response.statusCode == 200) {
      return (jsonDecode(response.body) as Map<String, dynamic>)['token'] as String;
    }
    _throwDetailedError(response);
  }

  /// POST /games
  /// Create a new game table
  Future<Map<String, dynamic>> createGame(String name, int smallBlind, int bigBlind) async {
    final response = await http.post(
      Uri.parse('$baseUrl/games'),
      headers: _headers,
      body: jsonEncode({
        'name': name,
        'blinds': {
          'small': smallBlind,
          'big': bigBlind,
        },
      }),
    );

    if (response.statusCode == 201) {
      return jsonDecode(response.body) as Map<String, dynamic>;
    } else {
      _throwDetailedError(response);
    }
  }

  /// GET /games/{gameId}
  /// Get game details. Sends the auth token when available so the response
  /// includes the personalized "you" betting context.
  Future<Map<String, dynamic>> getGame(String gameId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/games/$gameId'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body) as Map<String, dynamic>;
    } else {
      _throwDetailedError(response);
    }
  }

  /// POST /games/{gameId}/players
  /// Register/Buy-in a player to the game
  Future<void> buyIn(String gameId, int buyInAmount) async {
    final response = await http.post(
      Uri.parse('$baseUrl/games/$gameId/players'),
      headers: _headers,
      body: jsonEncode({
        'buyInAmount': buyInAmount,
      }),
    );

    if (response.statusCode == 204) {
      return;
    } else {
      _throwDetailedError(response);
    }
  }

  /// POST /games/{gameId}/deals
  /// Start a new hand: the server automatically posts blinds and deals hole cards.
  Future<Map<String, dynamic>> startDeal(String gameId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/games/$gameId/deals'),
      headers: _headers,
    );

    if (response.statusCode == 201) {
      return jsonDecode(response.body) as Map<String, dynamic>;
    } else {
      _throwDetailedError(response);
    }
  }

  /// POST /deals/{dealId}/actions
  /// Take a gameplay action
  Future<void> takeAction({
    required String dealId,
    required String actionType,
    int? amount,
  }) async {
    final bodyMap = <String, dynamic>{'actionType': actionType};
    if (amount != null) {
      bodyMap['amount'] = amount;
    }

    final response = await http.post(
      Uri.parse('$baseUrl/deals/$dealId/actions'),
      headers: _headers,
      body: jsonEncode(bodyMap),
    );

    if (response.statusCode == 204) {
      return;
    } else {
      _throwDetailedError(response);
    }
  }

  /// POST /games/{gameId}/close
  /// Ends the table for good: no further joins, deals, or actions afterwards.
  Future<void> closeTable(String gameId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/games/$gameId/close'),
      headers: _headers,
    );

    if (response.statusCode == 204) {
      return;
    } else {
      _throwDetailedError(response);
    }
  }

  /// POST /games/{gameId}/leave
  /// Stands the caller up from the table for good - excluded from future deals, and folded out of
  /// whichever hand is in progress if they were dealt into it.
  Future<void> leaveTable(String gameId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/games/$gameId/leave'),
      headers: _headers,
    );

    if (response.statusCode == 204) {
      return;
    } else {
      _throwDetailedError(response);
    }
  }

  /// GET /deals/{dealId}/hand/me
  /// Fetch the calling player's private cards
  Future<Map<String, dynamic>> getMyHand(String dealId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/deals/$dealId/hand/me'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body) as Map<String, dynamic>;
    } else {
      _throwDetailedError(response);
    }
  }

  Never _throwDetailedError(http.Response response) {
    try {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      final message = body['message'] ?? body['error'] ?? 'Unknown error';
      final code = body['code'] ?? 'HTTP_${response.statusCode}';
      throw ApiException(code: code.toString(), message: message.toString());
    } catch (e) {
      if (e is ApiException) {
        rethrow;
      }
      throw ApiException(
        code: 'HTTP_${response.statusCode}',
        message: response.body.isNotEmpty ? response.body : 'Request failed with status ${response.statusCode}',
      );
    }
  }
}

class ApiException implements Exception {
  final String code;
  final String message;

  ApiException({required this.code, required this.message});

  @override
  String toString() => '$code: $message';
}
