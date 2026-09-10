package vendredi.soir.karata.marketplace;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.service.JwtService;

/**
 * Separate from MarketplaceController since a purchase isn't addressed under a listing's own id.
 */
@RestController
@RequestMapping("/marketplace/purchases")
@AllArgsConstructor
public class PurchaseController {
  private final MarketplaceService marketplaceService;
  private final JwtService jwtService;

  /**
   * Re-checks payment status server-side as a side effect - poll this while waiting on a purchase.
   */
  @GetMapping("/{id}")
  public PurchaseResponse get(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable UUID id) {
    jwtService.validateAndExtractUsername(authHeader);
    return toResponse(marketplaceService.checkAndComplete(id));
  }

  static PurchaseResponse toResponse(ListingPurchase p) {
    return new PurchaseResponse(
        p.getId().toString(),
        p.getListingId().toString(),
        p.getBuyerUsername(),
        p.getQuantity(),
        p.getTotalPriceAr(),
        p.getStatus().name());
  }

  public record PurchaseResponse(
      String id,
      String listingId,
      String buyerUsername,
      long quantity,
      long totalPriceAr,
      String status) {}
}
