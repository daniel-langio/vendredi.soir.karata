package vendredi.soir.karata.marketplace;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Chips a seller has put up for sale, at a price the seller chose (the buyer can't negotiate it -
 * that's the "fixed price" part). Creating a listing escrows `chipsAmount` out of the seller's main
 * wallet immediately (see MarketplaceService) - it lives here, not in the wallet, until the listing
 * is sold (credited to the buyer) or cancelled (refunded to the seller).
 */
@Entity
@Table(name = "chip_listing")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChipListing {
  @Id private UUID id;

  private String sellerUsername;
  private long chipsAmount;
  private long priceAr;
  private String receivingPhoneNumber;

  @Enumerated(EnumType.STRING)
  private PaymentProvider provider;

  @Enumerated(EnumType.STRING)
  private ListingStatus status;

  private String buyerUsername;
  private String ifayPaymentId;

  private Instant createdAt;
  private Instant soldAt;
}
