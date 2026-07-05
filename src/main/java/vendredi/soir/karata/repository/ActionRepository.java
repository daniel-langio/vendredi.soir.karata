package vendredi.soir.karata.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vendredi.soir.karata.repository.model.ActionEntity;

@Repository
public interface ActionRepository extends JpaRepository<ActionEntity, UUID> {
  List<ActionEntity> findByGameIdOrderByOrderAsc(UUID gameId);

  List<ActionEntity> findByDealIdOrderByOrderAsc(UUID dealId);
}
