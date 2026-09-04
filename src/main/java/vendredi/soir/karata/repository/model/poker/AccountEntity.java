package vendredi.soir.karata.repository.model.poker;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "poker_account")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountEntity {
  @Id private UUID id;

  @Column(unique = true)
  private String username;

  private String passwordHash;
}
