package vendredi.soir.karata.marketplace;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.endpoint.rest.exception.BadRequestException;
import vendredi.soir.karata.service.JwtService;

@RestController
@RequestMapping("/marketplace/listings")
@AllArgsConstructor
public class MarketplaceController {
  private final MarketplaceService marketplaceService;
  private final JwtService jwtService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ListingResponse create(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody CreateListingRequest r) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    validate(r);
    return toResponse(
        marketplaceService.createListing(
            username, r.chipsAmount(), r.priceAr(), r.receivingPhoneNumber(), r.provider()));
  }

  @GetMapping
  public List<ListingResponse> listActive(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    jwtService.validateAndExtractUsername(authHeader);
    return marketplaceService.listActive().stream().map(this::toResponse).toList();
  }

  @DeleteMapping("/{id}")
  public ListingResponse cancel(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable UUID id) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    return toResponse(marketplaceService.cancelListing(username, id));
  }

  @PostMapping("/{id}/purchases")
  @ResponseStatus(HttpStatus.CREATED)
  public ListingResponse buy(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable UUID id,
      @RequestBody InitiatePurchaseRequest r) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    if (r == null || r.buyerPhoneNumber() == null || r.buyerPhoneNumber().isBlank()) {
      throw new BadRequestException("buyerPhoneNumber is required");
    }
    if (r.pspRef() == null || r.pspRef().isBlank()) {
      throw new BadRequestException("pspRef is required");
    }
    return toResponse(
        marketplaceService.initiatePurchase(username, id, r.buyerPhoneNumber(), r.pspRef()));
  }

  @GetMapping("/{id}")
  public ListingResponse get(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable UUID id) {
    jwtService.validateAndExtractUsername(authHeader);
    return toResponse(marketplaceService.checkAndComplete(id));
  }

  private void validate(CreateListingRequest r) {
    if (r == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (r.chipsAmount() == null || r.chipsAmount() <= 0) {
      throw new BadRequestException("chipsAmount must be strictly positive");
    }
    if (r.priceAr() == null || r.priceAr() <= 0) {
      throw new BadRequestException("priceAr must be strictly positive");
    }
    if (r.receivingPhoneNumber() == null || r.receivingPhoneNumber().isBlank()) {
      throw new BadRequestException("receivingPhoneNumber is required");
    }
    if (r.provider() == null) {
      throw new BadRequestException("provider is required");
    }
  }

  private ListingResponse toResponse(ChipListing l) {
    return new ListingResponse(
        l.getId().toString(),
        l.getSellerUsername(),
        l.getChipsAmount(),
        l.getPriceAr(),
        l.getProvider().name(),
        l.getReceivingPhoneNumber(),
        l.getStatus().name(),
        l.getBuyerUsername());
  }

  public record CreateListingRequest(
      Long chipsAmount, Long priceAr, String receivingPhoneNumber, PaymentProvider provider) {}

  public record InitiatePurchaseRequest(String buyerPhoneNumber, String pspRef) {}

  public record ListingResponse(
      String id,
      String sellerUsername,
      long chipsAmount,
      long priceAr,
      String provider,
      String receivingPhoneNumber,
      String status,
      String buyerUsername) {}
}
