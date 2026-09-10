package vendredi.soir.karata.marketplace;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChipListingRepository extends JpaRepository<ChipListing, UUID> {
  List<ChipListing> findByStatus(ListingStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from ChipListing l where l.id = :id")
  Optional<ChipListing> findByIdForUpdate(@Param("id") UUID id);
}
