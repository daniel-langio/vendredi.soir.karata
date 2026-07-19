package vendredi.soir.karata.repository.model.poker;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "poker_player")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerEntity {
  @Id private UUID id;
  private UUID gameId;
  private String username;
  private Long initialChips;
}
