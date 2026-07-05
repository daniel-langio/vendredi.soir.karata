package vendredi.soir.karata.repository.poker;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vendredi.soir.karata.repository.model.poker.GameEntity;
public interface GameRepository extends JpaRepository<GameEntity, UUID> {}
