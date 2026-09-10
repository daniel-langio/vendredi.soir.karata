package vendredi.soir.karata.marketplace;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * One buyer's order for some quantity of a listing's chips - a listing can have many of these over
 * time (or concurrently, from different buyers), unlike the listing itself which is a single row.
 * `totalPriceAr` is snapshotted at purchase time (quantity * the listing's unitPriceAr when this
 * was created) so the record stays meaningful even if the listing's own fields ever change.
 */
@Entity
@Table(name = "listing_purchase")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListingPurchase {
  @Id private UUID id;

  private UUID listingId;
  private String buyerUsername;
  private long quantity;
  private long totalPriceAr;

  @Enumerated(EnumType.STRING)
  private PurchaseStatus status;

  private String ifayPaymentId;

  private Instant createdAt;
  private Instant completedAt;
}
