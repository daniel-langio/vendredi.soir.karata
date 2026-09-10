package vendredi.soir.karata.marketplace;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingPurchaseRepository extends JpaRepository<ListingPurchase, UUID> {
  List<ListingPurchase> findByStatus(PurchaseStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from ListingPurchase p where p.id = :id")
  Optional<ListingPurchase> findByIdForUpdate(@Param("id") UUID id);
}
