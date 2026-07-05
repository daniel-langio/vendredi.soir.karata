package vendredi.soir.karata.repository.poker;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vendredi.soir.karata.repository.model.poker.ActionEntity;
public interface ActionRepository extends JpaRepository<ActionEntity, UUID> {
  List<ActionEntity> findByGameIdOrderByActionOrderAsc(UUID gameId);
  List<ActionEntity> findByDealIdOrderByActionOrderAsc(UUID dealId);
}
