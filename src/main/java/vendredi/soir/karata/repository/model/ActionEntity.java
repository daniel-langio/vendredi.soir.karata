package vendredi.soir.karata.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  @Column(name = "action_order")
  private Integer order;

  private String type;
  private String payload; // JSON representation of the action
  private Instant timestamp;
}
