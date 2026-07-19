package vendredi.soir.karata.repository.poker;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vendredi.soir.karata.repository.model.poker.GameEntity;

public interface GameRepository extends JpaRepository<GameEntity, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from GameEntity g where g.id = :id")
  Optional<GameEntity> findByIdForUpdate(@Param("id") UUID id);
}
