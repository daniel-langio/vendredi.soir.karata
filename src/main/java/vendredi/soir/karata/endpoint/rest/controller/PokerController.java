package vendredi.soir.karata.endpoint.rest.controller;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.endpoint.rest.mapper.RestMapper;
import vendredi.soir.karata.endpoint.rest.model.*;
import vendredi.soir.karata.repository.poker.GameRepository;
import vendredi.soir.karata.repository.model.poker.GameEntity;
import vendredi.soir.karata.service.*;
@RestController @RequestMapping("/poker") @AllArgsConstructor
public class PokerController {
  private final GameService gs;
  private final DealService ds;
  private final GameRepository gr;
  private final RestMapper rm;
  @PostMapping("/games") @ResponseStatus(HttpStatus.CREATED) public Game create(@RequestBody CreateGameRequest r) {
    GameEntity ge = gs.createGame(r.name(), r.blinds().small(), r.blinds().big());
    return rm.toRest(gs.getGame(ge.getId()), ge);
  }
  @GetMapping("/games/{gid}") public Game get(@PathVariable UUID gid) { return rm.toRest(gs.getGame(gid), gr.findById(gid).orElseThrow()); }
  @PostMapping("/games/{gid}/players") @ResponseStatus(HttpStatus.NO_CONTENT) public void join(@PathVariable UUID gid, @RequestBody JoinRequest r) { gs.joinGame(gid, r.username(), r.buyInAmount()); }
  @PostMapping("/games/{gid}/deals") @ResponseStatus(HttpStatus.CREATED) public Game start(@PathVariable UUID gid) {
    ds.startDeal(gid);
    return get(gid);
  }
  @PostMapping("/deals/{did}/actions") @ResponseStatus(HttpStatus.NO_CONTENT) public void action(@PathVariable UUID did, @RequestBody ActionRequest r) { ds.takeAction(did, r); }
  @GetMapping("/deals/{did}/hand/me") public Hand hand(@PathVariable UUID did, @RequestHeader("X-Username") String user) { return ds.getHand(did, user); }
  public record CreateGameRequest(String name, Blinds blinds) {}
  public record JoinRequest(String username, Long buyInAmount) {}
}
