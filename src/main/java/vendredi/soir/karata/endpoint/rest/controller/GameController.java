package vendredi.soir.karata.endpoint.rest.controller;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.endpoint.rest.mapper.RestMapper;
import vendredi.soir.karata.endpoint.rest.model.Blinds;
import vendredi.soir.karata.endpoint.rest.model.Game;
import vendredi.soir.karata.repository.model.GameEntity;
import vendredi.soir.karata.service.GameService;

@RestController
@RequestMapping("/poker/games")
@AllArgsConstructor
public class GameController {
  private final GameService gameService;
  private final RestMapper restMapper;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Game createGame(@RequestBody CreateGameRequest request) {
    GameEntity entity =
        gameService.createGame(request.name(), request.blinds().small(), request.blinds().big());
    return restMapper.toRest(gameService.getGame(entity.getId()), entity.getId());
  }

  @GetMapping("/{gameId}")
  public Game getGame(@PathVariable UUID gameId) {
    return restMapper.toRest(gameService.getGame(gameId), gameId);
  }

  @PostMapping("/{gameId}/players")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void joinGame(@PathVariable UUID gameId, @RequestBody JoinGameRequest request) {
    // TODO: implement
  }

  public record CreateGameRequest(String name, Blinds blinds) {}

  public record JoinGameRequest(Long buyInAmount) {}
}
