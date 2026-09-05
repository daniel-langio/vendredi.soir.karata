package vendredi.soir.karata.repository.poker;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vendredi.soir.karata.repository.model.poker.PlayerEntity;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {
  List<PlayerEntity> findByGameId(UUID gameId);

  Optional<PlayerEntity> findByGameIdAndUsername(UUID gameId, String username);
}
