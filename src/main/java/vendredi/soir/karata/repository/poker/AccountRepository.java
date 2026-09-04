package vendredi.soir.karata.repository.poker;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vendredi.soir.karata.repository.model.poker.AccountEntity;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
  Optional<AccountEntity> findByUsername(String username);
}
