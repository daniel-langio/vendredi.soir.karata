package vendredi.soir.karata.marketplace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP wrapper around ifay's client-facing API - no error translation here, callers handle it.
 */
@Component
class IfayClient {
  private final RestClient restClient;

  IfayClient(
      @Value("${ifay.base-url}") String baseUrl, @Value("${ifay.client-api-key}") String apiKey) {
    this.restClient =
        RestClient.builder().baseUrl(baseUrl).defaultHeader("X-Api-Key", apiKey).build();
  }

  ClaimResponse submitClaim(
      String senderPhone, String receiverPhone, long amount, PaymentProvider type, String pspRef) {
    return restClient
        .post()
        .uri("/payments/claims")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ClaimRequest(senderPhone, receiverPhone, amount, type, pspRef))
        .retrieve()
        .body(ClaimResponse.class);
  }

  ClaimResponse getClaim(String paymentId) {
    return restClient
        .get()
        .uri("/payments/claims/{id}", paymentId)
        .retrieve()
        .body(ClaimResponse.class);
  }

  record ClaimRequest(
      String senderPhone, String receiverPhone, long amount, PaymentProvider type, String pspRef) {}

  record ClaimResponse(String id, String status, Long amount) {}
}
