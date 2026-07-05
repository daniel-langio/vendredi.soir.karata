package vendredi.soir.karata.repository.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "poker_game")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameEntity {
  @Id private UUID id;
  private String name;
  private Long smallBlind;
  private Long bigBlind;
}
