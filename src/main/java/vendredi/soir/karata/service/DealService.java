package vendredi.soir.karata.service;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vendredi.soir.karata.endpoint.rest.model.ActionRequest;

@Service
@AllArgsConstructor
public class DealService {
  private final GameService gameService;

  public void takeAction(UUID dealId, ActionRequest actionRequest) {
    // TODO: implement action execution and persistence
  }

  public vendredi.soir.karata.endpoint.rest.model.Hand getPlayerHand(UUID dealId) {
    // TODO: implement
    return null;
  }
}
