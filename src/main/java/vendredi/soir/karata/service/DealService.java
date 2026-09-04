package vendredi.soir.karata.service;

import java.time.Instant;
import java.util.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.karata.core.action.*;
import vendredi.soir.karata.core.entity.*;
import vendredi.soir.karata.endpoint.rest.exception.*;
import vendredi.soir.karata.endpoint.rest.model.ActionRequest;
import vendredi.soir.karata.repository.model.poker.GameEntity;

@Service
@AllArgsConstructor
public class DealService {
  private final GameService gs;

  @Transactional
  public void takeAction(UUID did, ActionRequest req) {
    Game g = gs.getGameByDealId(did);
    Deal d = g.getCurrentDeal();
    Player p = g.getRules().determineNextPlayer(d, g.getPlayers());
    takeAction(did, req, p != null ? p.getName() : null);
  }

  @Transactional
  public void takeAction(UUID did, ActionRequest req, String username) {
    if (username == null) {
      throw new ForbiddenException("Unauthorized action: username is missing");
    }

    // Load game state with transactional locking
    UUID gid = gs.getGameIdByDealId(did);
    gs.lockGame(gid);

    Game g = gs.getGame(gid);
    Deal d = g.getCurrentDeal();
    if (d == null) {
      throw new BadRequestException("No active deal");
    }

    // Verify player is in the game
    Player player =
        g.getPlayers().stream()
            .filter(p -> p.getName().equals(username))
            .findFirst()
            .orElseThrow(() -> new ForbiddenException("Player is not registered in this game"));

    // Verify it is this player's turn
    Player nextPlayer = g.getRules().determineNextPlayer(d, g.getPlayers());
    if (nextPlayer == null) {
      throw new BadRequestException("No active turn or deal is over");
    }
    if (!nextPlayer.getName().equals(username)) {
      throw new ForbiddenException("Acting out of turn");
    }

    // Server-enforced timer check
    if (req.timeoutLimit() != null && Instant.now().isAfter(req.timeoutLimit())) {
      throw new BadRequestException("Action timed out");
    }

    long amt = req.amount() != null ? req.amount() : 0L;

    Action a =
        switch (req.actionType().toUpperCase()) {
          case "CHECK" -> new Check(player);
          case "CALL" -> new Call(player, amt);
          case "FOLD" -> new Fold(player);
          case "RAISE" -> new Raise(player, amt);
          case "BET" -> new Bet(player, amt);
          default -> throw new BadRequestException("Unknown action type: " + req.actionType());
        };

    try {
      saveAll(gid, did, g.getDealer().execute(g, d, a));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Illegal move: " + e.getMessage());
    }

    progressDealIfNeeded(g, d, gid, did);
  }

  @Transactional
  public void startDeal(UUID gid) {
    GameEntity ge = gs.lockGame(gid);
    Game g = gs.getGame(gid);

    System.out.println(
        "[DEBUG] players="
            + g.getPlayers().stream().map(p -> "'" + p.getName() + "'=" + g.getChips(p)).toList()
            + " initActions="
            + g.getHistory().stream()
                .filter(a -> a instanceof InitializePlayerChips)
                .map(
                    a -> {
                      InitializePlayerChips ipc = (InitializePlayerChips) a;
                      return "player="
                          + ipc.getPlayer()
                          + " name="
                          + (ipc.getPlayer() == null ? "NULL" : ipc.getPlayer().getName())
                          + " amount="
                          + ipc.getAmount();
                    })
                .toList());
    List<Player> eligible = g.getPlayers().stream().filter(p -> g.getChips(p) > 0).toList();
    if (eligible.size() < 2) {
      throw new BadRequestException("At least 2 players with chips are required to start a deal");
    }

    Deal d = g.startNewDeal(Deck.CLASSIC);
    UUID did = UUID.randomUUID();
    g.setCurrentDealId(did);

    d.getDeck().shuffle();
    Action shuffle = new ShuffleDeck(d.getDeck().getCards());
    saveAll(gid, did, g.getDealer().execute(g, d, shuffle));

    Player smallBlindPlayer = eligible.get(0);
    Player bigBlindPlayer = eligible.get(1);

    Action smallBlind = new SmallBlind(smallBlindPlayer, ge.getSmallBlind());
    saveAll(gid, did, g.getDealer().execute(g, d, smallBlind));

    Action bigBlind = new BigBlind(bigBlindPlayer, ge.getBigBlind());
    saveAll(gid, did, g.getDealer().execute(g, d, bigBlind));

    for (int round = 0; round < 2; round++) {
      for (Player p : eligible) {
        Card card = d.nextCards(1).get(0);
        Action holeCard = new DealHoleCard(p, card);
        saveAll(gid, did, g.getDealer().execute(g, d, holeCard));
      }
    }
  }

  /**
   * Advances the deal to the next street (or triggers the showdown) once the current betting round
   * is complete, or immediately awards the pot if every other player has folded.
   */
  private void progressDealIfNeeded(Game g, Deal d, UUID gid, UUID did) {
    if ("SHOWDOWN".equals(d.getCurrentPhase())) {
      return;
    }

    List<Player> active = g.getPlayers().stream().filter(p -> !d.hasFolded(p)).toList();
    boolean foldedOut = active.size() <= 1;
    if (!foldedOut && !g.getRules().isBettingRoundComplete(d, g.getPlayers())) {
      return;
    }

    Action next;
    if (foldedOut) {
      next = new Showdown();
    } else {
      next =
          switch (d.getCurrentPhase()) {
            case "PRE_FLOP" -> new RevealCards(d.nextCards(3));
            case "FLOP" -> new RevealCards(d.nextCards(1));
            case "TURN" -> new RevealCards(d.nextCards(1));
            default -> new Showdown(); // RIVER betting closed
          };
    }

    saveAll(gid, did, g.getDealer().execute(g, d, next));
  }

  private void saveAll(UUID gid, UUID did, List<Action> actions) {
    actions.forEach(a -> gs.saveAction(gid, did, a));
  }

  public vendredi.soir.karata.endpoint.rest.model.Hand getHand(UUID did, String user) {
    if (user == null) {
      throw new ForbiddenException("Unauthorized hand access: user is null");
    }
    Game g = gs.getGameByDealId(did);
    Deal currentDeal = g.getCurrentDeal();
    if (currentDeal == null) {
      throw new NotFoundException("No active deal found");
    }
    Player player =
        g.getPlayers().stream()
            .filter(p -> p.getName().equals(user))
            .findFirst()
            .orElseThrow(() -> new ForbiddenException("Player is not in this game"));

    List<Card> holeCards = currentDeal.getHoleCards(player);
    return new vendredi.soir.karata.endpoint.rest.model.Hand(
        holeCards.stream().map(Object::toString).toList());
  }
}
