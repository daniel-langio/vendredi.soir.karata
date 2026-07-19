package vendredi.soir.karata.repository.model.poker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import vendredi.soir.karata.core.action.Action;

@Component
@AllArgsConstructor
public class ActionMapper {
  private final ObjectMapper objectMapper;

  public ActionEntity toEntity(UUID gameId, UUID dealId, Action action) {
    try {
      return ActionEntity.builder()
          .id(UUID.randomUUID())
          .gameId(gameId)
          .dealId(dealId)
          .type(action.getClass().getSimpleName())
          .payload(objectMapper.writeValueAsString(action))
          .timestamp(java.time.Instant.now())
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public Action toDomain(ActionEntity entity) {
    try {
      return (Action)
          objectMapper.readValue(
              entity.getPayload(),
              Class.forName("vendredi.soir.karata.core.action." + entity.getType()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
