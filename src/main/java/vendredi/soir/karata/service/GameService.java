package vendredi.soir.karata.service;

import java.util.ArrayList;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vendredi.soir.karata.core.entity.Game;
import vendredi.soir.karata.core.rules.TexasHoldemRules;
import vendredi.soir.karata.repository.ActionRepository;
import vendredi.soir.karata.repository.GameRepository;
import vendredi.soir.karata.repository.model.GameEntity;

@Service
@AllArgsConstructor
public class GameService {
  private final GameRepository gameRepository;
  private final ActionRepository actionRepository;

  public GameEntity createGame(String name, Long smallBlind, Long bigBlind) {
    GameEntity entity =
        GameEntity.builder()
            .id(UUID.randomUUID())
            .name(name)
            .smallBlind(smallBlind)
            .bigBlind(bigBlind)
            .build();
    return gameRepository.save(entity);
  }

  public Game getGame(UUID gameId) {
    GameEntity entity =
        gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game not found"));

    // Rebuild core Game from actions (Simplified for now)
    Game game = new Game(new ArrayList<>(), new TexasHoldemRules());
    // TODO: replay history from actionRepository
    return game;
  }
}
