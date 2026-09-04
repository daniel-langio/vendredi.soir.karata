package vendredi.soir.karata.endpoint.rest.controller;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.endpoint.rest.exception.BadRequestException;
import vendredi.soir.karata.endpoint.rest.mapper.RestMapper;
import vendredi.soir.karata.endpoint.rest.model.*;
import vendredi.soir.karata.repository.model.poker.GameEntity;
import vendredi.soir.karata.repository.poker.GameRepository;
import vendredi.soir.karata.service.*;

@RestController
@RequestMapping("/poker")
@AllArgsConstructor
public class PokerController {
  private final GameService gs;
  private final DealService ds;
  private final GameRepository gr;
  private final RestMapper rm;
  private final JwtService jwtService;

  @PostMapping("/games")
  @ResponseStatus(HttpStatus.CREATED)
  public Game create(@RequestBody CreateGameRequest r) {
    validateCreateGame(r);
    GameEntity ge = gs.createGame(r.name(), r.blinds().small(), r.blinds().big());
    return rm.toRest(gs.getGame(ge.getId()), ge, null, null);
  }

  @GetMapping("/games/{gid}")
  public Game get(
      @PathVariable UUID gid,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String username = authHeader != null ? jwtService.validateAndExtractUsername(authHeader) : null;
    ds.enforceTurnTimeout(gid);
    vendredi.soir.karata.core.entity.Game g = gs.getGame(gid);
    UUID currentDealId = g.getCurrentDealId();
    Instant turnDeadline =
        currentDealId != null ? ds.currentTurnDeadline(gid, currentDealId) : null;
    return rm.toRest(g, gr.findById(gid).orElseThrow(), username, turnDeadline);
  }

  @PostMapping("/games/{gid}/players")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void join(
      @PathVariable UUID gid,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody JoinRequest r) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    validateJoinGame(r);
    gs.joinGame(gid, username, r.buyInAmount());
  }

  @PostMapping("/games/{gid}/close")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void close(
      @PathVariable UUID gid,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    gs.closeGame(gid, username);
  }

  @PostMapping("/games/{gid}/deals")
  @ResponseStatus(HttpStatus.CREATED)
  public Game start(
      @PathVariable UUID gid,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    ds.startDeal(gid);
    return get(gid, authHeader);
  }

  @PostMapping("/deals/{did}/actions")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void action(
      @PathVariable UUID did,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody ActionRequest r) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    validateAction(r);
    ds.takeAction(did, r, username);
  }

  @GetMapping("/deals/{did}/hand/me")
  public Hand hand(
      @PathVariable UUID did,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    return ds.getHand(did, username);
  }

  private void validateCreateGame(CreateGameRequest r) {
    if (r == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (r.name() == null || r.name().trim().isEmpty()) {
      throw new BadRequestException("Game name is required");
    }
    if (r.blinds() == null) {
      throw new BadRequestException("Blinds configuration is required");
    }
    if (r.blinds().small() == null || r.blinds().small() <= 0) {
      throw new BadRequestException("Small blind must be strictly positive");
    }
    if (r.blinds().big() == null || r.blinds().big() <= 0) {
      throw new BadRequestException("Big blind must be strictly positive");
    }
    if (r.blinds().big() < r.blinds().small()) {
      throw new BadRequestException("Big blind must be greater than or equal to small blind");
    }
  }

  private void validateJoinGame(JoinRequest r) {
    if (r == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (r.buyInAmount() == null || r.buyInAmount() <= 0) {
      throw new BadRequestException("Buy-in amount must be strictly positive");
    }
  }

  private void validateAction(ActionRequest r) {
    if (r == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (r.actionType() == null) {
      throw new BadRequestException("Action type is required");
    }
    String type = r.actionType().toUpperCase();
    if (!type.equals("CHECK")
        && !type.equals("CALL")
        && !type.equals("FOLD")
        && !type.equals("RAISE")
        && !type.equals("BET")) {
      throw new BadRequestException("Invalid action type: " + r.actionType());
    }
    if ((type.equals("RAISE") || type.equals("BET")) && (r.amount() == null || r.amount() <= 0)) {
      throw new BadRequestException("Amount is required and must be strictly positive for " + type);
    }
  }

  public record CreateGameRequest(String name, Blinds blinds) {}

  public record JoinRequest(Long buyInAmount) {}
}
