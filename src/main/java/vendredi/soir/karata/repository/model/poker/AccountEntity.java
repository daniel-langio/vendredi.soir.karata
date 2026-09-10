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

  /**
   * Not verified/authoritative - purely a convenience default for the marketplace listing form.
   * Whatever phone number actually matters for a given payment is always the one entered on that
   * specific listing/purchase, matched via ifay.
   */
  private String phoneNumber;
}
