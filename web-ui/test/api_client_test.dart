import 'package:flutter_test/flutter_test.dart';
import 'package:poker_client/api/api_client.dart';

void main() {
  group('ApiClient Configuration Tests', () {
    const testBaseUrl = 'https://example.com/poker';
    const testToken = 'mock_jwt_token';

    test('should configure baseUrl and authorization headers correctly', () {
      final client = ApiClient(baseUrl: testBaseUrl, token: testToken);

      expect(client.baseUrl, testBaseUrl);
      expect(client.token, testToken);
    });

    test('should default to an empty token when none is supplied', () {
      final client = ApiClient(baseUrl: testBaseUrl);
      expect(client.token, isEmpty);
    });
  });
}
