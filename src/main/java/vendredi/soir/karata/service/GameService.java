package vendredi.soir.karata.service;

import java.util.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.karata.core.action.*;
import vendredi.soir.karata.core.entity.*;
import vendredi.soir.karata.core.rules.TexasHoldemRules;
import vendredi.soir.karata.repository.model.poker.*;
import vendredi.soir.karata.repository.poker.*;

@Service
@AllArgsConstructor
public class GameService {
  private final GameRepository gameRepository;
  private final PlayerRepository playerRepository;
  private final ActionRepository actionRepository;
  private final ActionMapper actionMapper;

  @Transactional
  public GameEntity createGame(String name, Long sb, Long bb) {
    return gameRepository.save(
        GameEntity.builder().id(UUID.randomUUID()).name(name).smallBlind(sb).bigBlind(bb).build());
  }

  @Transactional
  public GameEntity lockGame(UUID gid) {
    return gameRepository
        .findByIdForUpdate(gid)
        .orElseThrow(() -> new NoSuchElementException("Game not found with ID: " + gid));
  }

  @Transactional
  public void joinGame(UUID gid, String user, Long chips) {
    lockGame(gid);
    playerRepository.save(
        PlayerEntity.builder()
            .id(UUID.randomUUID())
            .gameId(gid)
            .username(user)
            .initialChips(chips)
            .build());
    saveAction(gid, null, new InitializePlayerChips(new Player(user), chips));
  }

  @Transactional(readOnly = true)
  public Game getGame(UUID gid) {
    GameEntity ge = gameRepository.findById(gid).orElseThrow();
    List<Player> players =
        playerRepository.findByGameId(gid).stream().map(p -> new Player(p.getUsername())).toList();
    Game game = new Game(players, new TexasHoldemRules());
    actionRepository
        .findByGameIdOrderByActionOrderAsc(gid)
        .forEach(
            ae -> {
              Action a = actionMapper.toDomain(ae);
              if (ae.getDealId() == null) game.addAction(a);
              else {
                if (game.getCurrentDeal() == null
                    || !ae.getDealId().equals(game.getCurrentDealId())) {
                  if (a instanceof ShuffleDeck sd && sd.getCards() != null) {
                    game.startNewDeal(new Deck(new ArrayList<>(sd.getCards())));
                  } else {
                    game.startNewDeal(Deck.CLASSIC);
                  }
                  game.setCurrentDealId(ae.getDealId());
                }
                game.getCurrentDeal().apply(a);
              }
            });
    return game;
  }

  @Transactional
  public void saveAction(UUID gid, UUID did, Action a) {
    actionRepository.save(actionMapper.toEntity(gid, did, a));
  }

  @Transactional(readOnly = true)
  public Game getGameByDealId(UUID did) {
    return getGame(getGameIdByDealId(did));
  }

  @Transactional(readOnly = true)
  public UUID getGameIdByDealId(UUID did) {
    List<ActionEntity> actions = actionRepository.findByDealIdOrderByActionOrderAsc(did);
    if (actions.isEmpty()) throw new NoSuchElementException("No actions for deal " + did);
    return actions.get(0).getGameId();
  }
}
