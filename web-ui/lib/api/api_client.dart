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
  /// Create a new game table. defaultBuyIn (if given) is kept by the server as the table's
  /// suggested buy-in for anyone joining later - see JoinTableScreen.
  Future<Map<String, dynamic>> createGame(
    String name,
    int smallBlind,
    int bigBlind, {
    int? defaultBuyIn,
    String? variant,
  }) async {
    final body = <String, dynamic>{
      'name': name,
      'blinds': {
        'small': smallBlind,
        'big': bigBlind,
      },
    };
    if (defaultBuyIn != null) body['defaultBuyIn'] = defaultBuyIn;
    if (variant != null) body['variant'] = variant;

    final response = await http.post(
      Uri.parse('$baseUrl/games'),
      headers: _headers,
      body: jsonEncode(body),
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
    List<String>? discard,
  }) async {
    final bodyMap = <String, dynamic>{'actionType': actionType};
    if (amount != null) {
      bodyMap['amount'] = amount;
    }
    if (discard != null) {
      bodyMap['discard'] = discard;
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

  /// GET /wallet
  /// Fetch the caller's persistent chip wallet balance (separate from any single table's
  /// in-progress stack).
  Future<int> getWallet() async {
    final response = await http.get(
      Uri.parse('$baseUrl/wallet'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      return (body['chips'] as num).toInt();
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

  /// GET /account
  /// The account's stored phone number, if any - purely a form-default convenience, never
  /// authoritative (the number that matters for matching is whatever's entered on a given
  /// listing/purchase).
  Future<String?> getAccountPhoneNumber() async {
    final response = await http.get(Uri.parse('$baseUrl/account'), headers: _headers);
    if (response.statusCode == 200) {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      return body['phoneNumber'] as String?;
    } else {
      _throwDetailedError(response);
    }
  }

  /// PUT /account/phone-number
  Future<void> setAccountPhoneNumber(String phoneNumber) async {
    final response = await http.put(
      Uri.parse('$baseUrl/account/phone-number'),
      headers: _headers,
      body: jsonEncode({'phoneNumber': phoneNumber}),
    );
    if (response.statusCode != 200) {
      _throwDetailedError(response);
    }
  }

  // The marketplace lives at the API root (sibling to /poker), not under it - baseUrl already
  // has /poker baked in (see WelcomeScreen.defaultServerUrl), so strip it back off here.
  String get _rootUrl =>
      baseUrl.endsWith('/poker') ? baseUrl.substring(0, baseUrl.length - '/poker'.length) : baseUrl;

  /// GET /marketplace/listings
  Future<List<Map<String, dynamic>>> listMarketplaceListings() async {
    final response = await http.get(Uri.parse('$_rootUrl/marketplace/listings'), headers: _headers);
    if (response.statusCode == 200) {
      return (jsonDecode(response.body) as List).cast<Map<String, dynamic>>();
    } else {
      _throwDetailedError(response);
    }
  }

  /// POST /marketplace/listings
  Future<Map<String, dynamic>> createListing({
    required int chipsAmount,
    required int priceAr,
    required String receivingPhoneNumber,
    required String provider,
  }) async {
    final response = await http.post(
      Uri.parse('$_rootUrl/marketplace/listings'),
      headers: _headers,
      body: jsonEncode({
        'chipsAmount': chipsAmount,
        'priceAr': priceAr,
        'receivingPhoneNumber': receivingPhoneNumber,
        'provider': provider,
      }),
    );
    if (response.statusCode == 201) {
      return jsonDecode(response.body) as Map<String, dynamic>;
    } else {
      _throwDetailedError(response);
    }
  }

  /// DELETE /marketplace/listings/{id}
  Future<void> cancelListing(String id) async {
    final response = await http.delete(Uri.parse('$_rootUrl/marketplace/listings/$id'), headers: _headers);
    if (response.statusCode != 200) {
      _throwDetailedError(response);
    }
  }

  /// POST /marketplace/listings/{id}/purchases
  Future<Map<String, dynamic>> buyListing({
    required String id,
    required String buyerPhoneNumber,
    required String pspRef,
  }) async {
    final response = await http.post(
      Uri.parse('$_rootUrl/marketplace/listings/$id/purchases'),
      headers: _headers,
      body: jsonEncode({'buyerPhoneNumber': buyerPhoneNumber, 'pspRef': pspRef}),
    );
    if (response.statusCode == 201) {
      return jsonDecode(response.body) as Map<String, dynamic>;
    } else {
      _throwDetailedError(response);
    }
  }

  /// GET /marketplace/listings/{id}
  /// Re-checks payment status server-side as a side effect - poll this while waiting for a
  /// purchase to verify.
  Future<Map<String, dynamic>> getListing(String id) async {
    final response = await http.get(Uri.parse('$_rootUrl/marketplace/listings/$id'), headers: _headers);
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
