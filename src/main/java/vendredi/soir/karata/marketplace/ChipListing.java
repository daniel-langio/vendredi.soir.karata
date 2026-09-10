package vendredi.soir.karata.marketplace;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Chips a seller has put up for sale, at a per-chip price the seller chose (the buyer can't
 * negotiate it - that's the "fixed price" part) - buyers then pick how many of the remaining chips
 * they want (see {@link ListingPurchase}). Creating a listing escrows the full `chipsAmount` out of
 * the seller's main wallet immediately (see MarketplaceService); `chipsAmount` here tracks what's
 * still unreserved as purchases are made, refunded to the seller on cancellation.
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

  /** Chips still available - decremented (reserved) the moment a buyer initiates a purchase. */
  private long chipsAmount;

  private long unitPriceAr;
  private String receivingPhoneNumber;

  @Enumerated(EnumType.STRING)
  private PaymentProvider provider;

  @Enumerated(EnumType.STRING)
  private ListingStatus status;

  private Instant createdAt;
}
