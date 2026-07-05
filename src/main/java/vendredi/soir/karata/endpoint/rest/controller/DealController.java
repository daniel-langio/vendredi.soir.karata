package vendredi.soir.karata.endpoint.rest.controller;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.endpoint.rest.model.ActionRequest;
import vendredi.soir.karata.endpoint.rest.model.Hand;
import vendredi.soir.karata.service.DealService;

@RestController
@RequestMapping("/poker/deals")
@AllArgsConstructor
public class DealController {
  private final DealService dealService;

  @PostMapping("/{dealId}/actions")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void takeAction(@PathVariable UUID dealId, @RequestBody ActionRequest request) {
    dealService.takeAction(dealId, request);
  }

  @GetMapping("/{dealId}/hand/me")
  public Hand getMyHand(@PathVariable UUID dealId) {
    return dealService.getPlayerHand(dealId);
  }
}
