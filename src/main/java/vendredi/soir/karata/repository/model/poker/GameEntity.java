package vendredi.soir.karata.repository.model.poker;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
@Entity @Table(name = "poker_game") @Data @Builder @AllArgsConstructor @NoArgsConstructor
public class GameEntity {
  @Id private UUID id;
  private String name;
  private Long smallBlind;
  private Long bigBlind;
}
