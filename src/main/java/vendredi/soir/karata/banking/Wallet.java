package vendredi.soir.karata.banking;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "wallet")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Wallet {
  @Id private UUID id;

  @Column(unique = true)
  private String username;

  private Long chips;
}
