package vendredi.soir.karata.core.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import vendredi.soir.karata.core.action.*;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Game;
import vendredi.soir.karata.core.entity.Hand;
import vendredi.soir.karata.core.entity.Player;
import vendredi.soir.karata.core.factory.HandFactory;

public class TexasHoldemRules implements Rules {

  @Override
  public boolean isActionLegal(Game game, Deal deal, Action action) {
    if (action instanceof PlayerAction pa) {
      // Folding must stay legal in every phase regardless of variant - enforceTurnTimeout relies
      // on always being able to bail an AFK player out, including mid-draw.
      if (pa instanceof Fold) {
        return true;
      }
      String phase = currentPhase(deal, deal.filterDealtIn(game.getPlayers()));
      if (pa instanceof Draw draw) {
        if (!"DRAW".equals(phase)) return false;
        List<Card> hand = deal.getHoleCards(draw.getPlayer());
        boolean allOwned = hand.containsAll(draw.getDiscarded());
        boolean notTooMany = draw.getDiscarded().size() <= hand.size();
        boolean noDuplicates =
            new HashSet<>(draw.getDiscarded()).size() == draw.getDiscarded().size();
        boolean alreadyDrew =
            deal.getHistory().stream()
                .anyMatch(a -> a instanceof Draw d && d.getPlayer().equals(draw.getPlayer()));
        return allOwned && notTooMany && noDuplicates && !alreadyDrew;
      }
      // No betting-style action is legal during a draw phase (nothing to bet on yet this round).
      if ("DRAW".equals(phase)) {
        return false;
      }
      if (pa instanceof Bet bet) {
        return game.getChips(bet.getPlayer()) >= bet.getAmount() && deal.getCurrentRoundBet() == 0;
      }
      if (pa instanceof Raise raise) {
        return game.getChips(raise.getPlayer()) >= raise.getAmount()
            && raise.getAmount() >= getMinimumRaise(deal);
      }
      if (pa instanceof Call call) {
        return game.getChips(call.getPlayer()) >= call.getAmount();
      }
      if (pa instanceof Check) {
        return deal.getCurrentRoundBet() == deal.getPlayerRoundContribution(pa.getPlayer());
      }
    }
    return true;
  }

  @Override
  public Player determineNextPlayer(Deal deal, List<Player> players) {
    if (deal.getHistory().stream().anyMatch(a -> a instanceof Showdown)) return null;

    List<Player> active = players.stream().filter(p -> !deal.hasFolded(p)).toList();

    if (active.isEmpty()) return null;

    // Simplified turn logic: find the last player who acted and pick the next one
    Player lastActor =
        deal.getHistory().stream()
            .filter(a -> a instanceof PlayerAction)
            .map(a -> ((PlayerAction) a).getPlayer())
            .reduce((first, second) -> second)
            .orElse(null);

    if (lastActor == null) return active.get(0);

    int lastIndex = players.indexOf(lastActor);
    for (int i = 1; i <= players.size(); i++) {
      Player next = players.get((lastIndex + i) % players.size());
      if (active.contains(next)) return next;
    }
    return null;
  }

  @Override
  public Map<Player, Hand> evaluateWinners(Deal deal, List<Player> players) {
    List<Player> active = players.stream().filter(p -> !deal.hasFolded(p)).toList();
    if (active.size() == 1) {
      // Every other player folded: the sole remaining player wins the pot uncontested,
      // without a card-based showdown.
      Map<Player, Hand> soleWinner = new HashMap<>();
      soleWinner.put(active.get(0), null);
      return soleWinner;
    }

    Map<Player, Hand> bestHands = evaluateShowdownHands(deal, players);
    if (bestHands.isEmpty()) return Map.of();

    Hand winningHand = bestHands.values().stream().max(Hand::compareTo).get();
    return bestHands.entrySet().stream()
        .filter(e -> e.getValue().compareTo(winningHand) == 0)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Map<Player, Hand> evaluateShowdownHands(Deal deal, List<Player> players) {
    Map<Player, Hand> bestHands = new HashMap<>();
    for (Player p : players) {
      if (!deal.hasFolded(p)) {
        List<Card> allCards = new ArrayList<>(deal.getHoleCards(p));
        allCards.addAll(deal.getBoard());
        if (allCards.size() >= 5) {
          bestHands.put(p, HandFactory.evaluateBestHand(allCards));
        }
      }
    }
    return bestHands;
  }

  @Override
  public long getMinimumRaise(Deal deal) {
    // Simplified: double the current bet
    return Math.max(20, deal.getCurrentRoundBet() * 2);
  }

  @Override
  public List<Player> getBettingOrder(List<Player> players) {
    return players;
  }

  @Override
  public boolean isBettingRoundComplete(Deal deal, List<Player> players) {
    List<Player> active = players.stream().filter(p -> !deal.hasFolded(p)).toList();
    if (active.size() <= 1) return true;

    long roundBet = deal.getCurrentRoundBet();
    Set<Player> acted =
        deal.getActionsInCurrentPhase().stream()
            .filter(a -> a instanceof PlayerAction)
            .map(a -> ((PlayerAction) a).getPlayer())
            .collect(Collectors.toSet());

    for (Player p : active) {
      if (!acted.contains(p)) return false;
      if (deal.getPlayerRoundContribution(p) != roundBet) return false;
    }
    return true;
  }

  @Override
  public String currentPhase(Deal deal, List<Player> dealtInPlayers) {
    if (deal.getHistory().stream().anyMatch(a -> a instanceof Showdown)) return "SHOWDOWN";
    long revealCount = deal.getHistory().stream().filter(a -> a instanceof RevealCards).count();
    if (revealCount == 0) return "PRE_FLOP";
    if (revealCount == 1) return "FLOP";
    if (revealCount == 2) return "TURN";
    return "RIVER";
  }

  @Override
  public boolean isCurrentPhaseComplete(Deal deal, List<Player> dealtInPlayers) {
    return isBettingRoundComplete(deal, dealtInPlayers);
  }

  @Override
  public Action nextPhaseAction(
      Game game, Deal deal, List<Player> dealtInPlayers, boolean foldedOut) {
    if (foldedOut) return new Showdown();
    return switch (currentPhase(deal, dealtInPlayers)) {
      case "PRE_FLOP" -> new RevealCards(deal.nextCards(3));
      case "FLOP" -> new RevealCards(deal.nextCards(1));
      case "TURN" -> new RevealCards(deal.nextCards(1));
      default -> new Showdown(); // RIVER betting closed
    };
  }
}
