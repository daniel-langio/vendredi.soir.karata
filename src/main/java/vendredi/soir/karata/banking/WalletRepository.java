package vendredi.soir.karata.banking;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
  Optional<Wallet> findByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select w from Wallet w where w.username = :username")
  Optional<Wallet> findByUsernameForUpdate(@Param("username") String username);
}
