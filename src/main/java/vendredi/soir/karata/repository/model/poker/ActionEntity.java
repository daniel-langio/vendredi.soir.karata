package vendredi.soir.karata.repository.model.poker;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
@Entity @Table(name = "poker_action") @Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ActionEntity {
  @Id private UUID id;
  private UUID gameId;
  private UUID dealId;
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "action_order", insertable = false, updatable = false)
  private Integer actionOrder;
  private String type;
  private String payload;
  private Instant timestamp;
}
