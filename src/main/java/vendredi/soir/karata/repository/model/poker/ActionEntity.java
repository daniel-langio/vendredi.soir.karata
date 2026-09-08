package vendredi.soir.karata.repository.model.poker;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "poker_action")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionEntity {
  @Id private UUID id;
  private UUID gameId;
  private UUID dealId;

  // Explicitly assigned by GameService.saveAction (see there for why) - NOT a DB-generated
  // identity column, despite an earlier version of this class claiming it was one via
  // @GeneratedValue(IDENTITY): the actual poker_action.action_order column was never created as
  // a real identity/serial column, so with insertable=false (the old annotation) every row's
  // action_order silently stayed NULL forever, and every replay of these actions across the
  // whole app has never had a guaranteed-correct order until this was fixed.
  @Column(name = "action_order", nullable = false)
  private Integer actionOrder;

  private String type;

  @Column(columnDefinition = "TEXT")
  private String payload;

  private Instant timestamp;
}
