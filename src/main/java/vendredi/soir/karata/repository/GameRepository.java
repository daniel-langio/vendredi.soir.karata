package vendredi.soir.karata.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vendredi.soir.karata.repository.model.GameEntity;

@Repository
public interface GameRepository extends JpaRepository<GameEntity, UUID> {}
