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
      g.getDealer().execute(g, d, a);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Illegal move: " + e.getMessage());
    }

    gs.saveAction(gid, did, a);
  }

  @Transactional
  public void startDeal(UUID gid) {
    gs.lockGame(gid);
    Game g = gs.getGame(gid);
    Deal d = g.startNewDeal(Deck.CLASSIC);
    UUID did = UUID.randomUUID();
    g.setCurrentDealId(did);
    Action s = new ShuffleDeck(d.getDeck().getCards());
    g.getDealer().execute(g, d, s);
    gs.saveAction(gid, did, s);
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
