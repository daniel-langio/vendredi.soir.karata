package vendredi.soir.karata.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.karata.banking.BankingService;
import vendredi.soir.karata.core.action.*;
import vendredi.soir.karata.core.entity.*;
import vendredi.soir.karata.core.rules.OmahaRules;
import vendredi.soir.karata.core.rules.Rules;
import vendredi.soir.karata.core.rules.TexasHoldemRules;
import vendredi.soir.karata.endpoint.rest.exception.ConflictException;
import vendredi.soir.karata.endpoint.rest.exception.ForbiddenException;
import vendredi.soir.karata.repository.model.poker.*;
import vendredi.soir.karata.repository.poker.*;

@Service
@AllArgsConstructor
public class GameService {
  private final GameRepository gameRepository;
  private final PlayerRepository playerRepository;
  private final ActionRepository actionRepository;
  private final ActionMapper actionMapper;
  private final BankingService bankingService;

  @Transactional
  public GameEntity createGame(String name, Long sb, Long bb, Long defaultBuyIn, String variant) {
    GameEntity.GameEntityBuilder builder =
        GameEntity.builder()
            .id(UUID.randomUUID())
            .name(name)
            .smallBlind(sb)
            .bigBlind(bb)
            .defaultBuyIn(defaultBuyIn);
    if (variant != null) {
      builder.variant(variant);
    }
    return gameRepository.save(builder.build());
  }

  private static Rules rulesFor(String variant) {
    return "OMAHA".equals(variant) ? new OmahaRules() : new TexasHoldemRules();
  }

  @Transactional
  public GameEntity lockGame(UUID gid) {
    return gameRepository
        .findByIdForUpdate(gid)
        .orElseThrow(() -> new NoSuchElementException("Game not found with ID: " + gid));
  }

  @Transactional
  public void joinGame(UUID gid, String user, Long chips) {
    GameEntity ge = lockGame(gid);
    if (Boolean.TRUE.equals(ge.getClosed())) {
      throw new ConflictException("Table is closed");
    }
    bankingService.debit(user, chips);
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
    Game game = new Game(players, rulesFor(ge.getVariant()));
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

  /**
   * Every persisted action goes through here, in a strictly increasing per-game order - callers
   * must already hold the per-game pessimistic lock (see lockGame) for the duration of their
   * transaction, since this computes "next" by reading the current max, not via a DB sequence.
   */
  @Transactional
  public void saveAction(UUID gid, UUID did, Action a) {
    int nextOrder =
        actionRepository
            .findTopByGameIdOrderByActionOrderDesc(gid)
            .map(e -> e.getActionOrder() + 1)
            .orElse(0);
    actionRepository.save(actionMapper.toEntity(gid, did, a, nextOrder));
  }

  /**
   * Ends a table for good: no further joins, deals, or actions are accepted afterwards. Anyone
   * seated at the table can close it - there's no host/owner concept beyond that. Every
   * still-active player is cashed out for whatever they currently have, back into their persistent
   * wallet.
   */
  @Transactional
  public void closeGame(UUID gid, String username) {
    GameEntity ge = lockGame(gid);
    if (Boolean.TRUE.equals(ge.getClosed())) {
      throw new ConflictException("Table is already closed");
    }
    List<PlayerEntity> players = playerRepository.findByGameId(gid);
    boolean seated = players.stream().anyMatch(p -> p.getUsername().equals(username));
    if (!seated) {
      throw new ForbiddenException("Only a seated player can close this table");
    }
    Game g = getGame(gid);
    players.stream()
        .filter(p -> !Boolean.FALSE.equals(p.getActive()))
        .forEach(
            pe -> {
              Player player =
                  g.getPlayers().stream()
                      .filter(p -> p.getName().equals(pe.getUsername()))
                      .findFirst()
                      .orElseThrow();
              bankingService.credit(pe.getUsername(), g.getChips(player));
            });
    ge.setClosed(true);
    gameRepository.save(ge);
  }

  @Transactional(readOnly = true)
  public Set<String> getActiveUsernames(UUID gid) {
    return playerRepository.findByGameId(gid).stream()
        .filter(p -> !Boolean.FALSE.equals(p.getActive()))
        .map(PlayerEntity::getUsername)
        .collect(Collectors.toSet());
  }

  /**
   * Marks a player as having left the table for good: excluded from future deals and from the
   * players list shown to clients. Does not touch any deal currently in progress - the caller is
   * responsible for folding them out of it first if needed (see DealService.leaveGame).
   */
  @Transactional
  public void markPlayerLeft(UUID gid, String username) {
    PlayerEntity pe =
        playerRepository
            .findByGameIdAndUsername(gid, username)
            .orElseThrow(() -> new ForbiddenException("Player is not registered in this game"));
    if (Boolean.FALSE.equals(pe.getActive())) {
      throw new ConflictException("Player has already left this table");
    }
    pe.setActive(false);
    playerRepository.save(pe);
  }

  /** Resets a player's consecutive-missed-turns count - called whenever they actually act. */
  @Transactional
  public void resetMissedTurns(UUID gid, String username) {
    playerRepository
        .findByGameIdAndUsername(gid, username)
        .ifPresent(
            pe -> {
              if (pe.getMissedTurns() != null && pe.getMissedTurns() != 0) {
                pe.setMissedTurns(0);
                playerRepository.save(pe);
              }
            });
  }

  /**
   * Records that a player's turn had to be auto-folded on timeout, and returns their new
   * consecutive-missed-turns count (0 if the player entity is somehow missing).
   */
  @Transactional
  public int incrementMissedTurns(UUID gid, String username) {
    Optional<PlayerEntity> found = playerRepository.findByGameIdAndUsername(gid, username);
    if (found.isEmpty()) return 0;
    PlayerEntity pe = found.get();
    int missed = (pe.getMissedTurns() == null ? 0 : pe.getMissedTurns()) + 1;
    pe.setMissedTurns(missed);
    playerRepository.save(pe);
    return missed;
  }

  @Transactional(readOnly = true)
  public Optional<Instant> getLastActionTimestamp(UUID did) {
    return actionRepository
        .findTopByDealIdOrderByActionOrderDesc(did)
        .map(ActionEntity::getTimestamp);
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
