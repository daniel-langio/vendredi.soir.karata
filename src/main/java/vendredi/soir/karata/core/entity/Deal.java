package vendredi.soir.karata.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import vendredi.soir.karata.core.action.*;

@Getter
public class Deal {
  private final Deck deck;
  private final List<Action> history;

  public Deal(Deck deck) {
    this.deck = new Deck(new ArrayList<>(deck.getCards()));
    this.history = new ArrayList<>();
  }

  public void apply(Action action) {
    history.add(action);
  }

  // Projections
  public List<Card> getHoleCards(Player player) {
    return history.stream()
        .filter(a -> a instanceof DealHoleCard dhc && dhc.getPlayer().equals(player))
        .map(a -> ((DealHoleCard) a).getCard())
        .collect(Collectors.toList());
  }

  public boolean hasFolded(Player player) {
    return history.stream().anyMatch(a -> a instanceof Fold f && f.getPlayer().equals(player));
  }

  public boolean isAllIn(Player player, Game game) {
    return game.getChips(player) == 0;
  }

  public boolean isSmallBlind(Player player) {
    return history.stream()
        .anyMatch(a -> a instanceof SmallBlind sb && sb.getPlayer().equals(player));
  }

  public boolean isBigBlind(Player player) {
    return history.stream()
        .anyMatch(a -> a instanceof BigBlind bb && bb.getPlayer().equals(player));
  }

  public long getContribution(Player player) {
    return history.stream()
        .filter(a -> a instanceof PlayerAction pa && pa.getPlayer().equals(player))
        .mapToLong(
            a -> {
              if (a instanceof Bet bet) return bet.getAmount();
              if (a instanceof Raise raise) return raise.getAmount();
              if (a instanceof Call call) return call.getAmount();
              if (a instanceof SmallBlind sb) return sb.getAmount();
              if (a instanceof BigBlind bb) return bb.getAmount();
              return 0L;
            })
        .sum();
  }

  public long getPlayerRoundContribution(Player player) {
    List<Action> roundActions = getActionsInCurrentPhase();
    return roundActions.stream()
        .filter(a -> a instanceof PlayerAction pa && pa.getPlayer().equals(player))
        .mapToLong(
            a -> {
              if (a instanceof Bet bet) return bet.getAmount();
              if (a instanceof Raise raise) return raise.getAmount();
              if (a instanceof Call call) return call.getAmount();
              if (a instanceof SmallBlind sb) return sb.getAmount();
              if (a instanceof BigBlind bb) return bb.getAmount();
              return 0L;
            })
        .sum();
  }

  public long getCurrentRoundBet() {
    List<Action> roundActions = getActionsInCurrentPhase();
    Map<Player, Long> contributions =
        roundActions.stream()
            .filter(a -> a instanceof PlayerAction)
            .map(a -> (PlayerAction) a)
            .collect(
                Collectors.groupingBy(
                    PlayerAction::getPlayer,
                    Collectors.summingLong(
                        a -> {
                          if (a instanceof Bet bet) return bet.getAmount();
                          if (a instanceof Raise raise) return raise.getAmount();
                          if (a instanceof Call call) return call.getAmount();
                          if (a instanceof SmallBlind sb) return sb.getAmount();
                          if (a instanceof BigBlind bb) return bb.getAmount();
                          return 0L;
                        })));
    return contributions.values().stream().max(Long::compare).orElse(0L);
  }

  public String getCurrentPhase() {
    if (history.stream().anyMatch(a -> a instanceof Showdown)) return "SHOWDOWN";
    long revealCount = history.stream().filter(a -> a instanceof RevealCards).count();
    if (revealCount == 0) return "PRE_FLOP";
    if (revealCount == 1) return "FLOP";
    if (revealCount == 2) return "TURN";
    return "RIVER";
  }

  public int getHoleCardsDealtCount() {
    return (int) history.stream().filter(a -> a instanceof DealHoleCard).count();
  }

  /**
   * Returns the next {@code count} cards to be dealt from the shuffled deck order, accounting for
   * hole cards and community cards already dealt in this deal's history.
   */
  public List<Card> nextCards(int count) {
    int consumed = getHoleCardsDealtCount() + getBoard().size();
    List<Card> order = deck.getCards();
    if (consumed + count > order.size()) {
      throw new IllegalStateException("Not enough cards remaining in the deck");
    }
    return new ArrayList<>(order.subList(consumed, consumed + count));
  }

  public List<Action> getActionsInCurrentPhase() {
    int lastRevealIndex = -1;
    for (int i = history.size() - 1; i >= 0; i--) {
      if (history.get(i) instanceof RevealCards) {
        lastRevealIndex = i;
        break;
      }
    }
    return history.subList(lastRevealIndex + 1, history.size());
  }

  public long getTotalPot() {
    return history.stream()
        .mapToLong(
            a -> {
              if (a instanceof Bet bet) return bet.getAmount();
              if (a instanceof Raise raise) return raise.getAmount();
              if (a instanceof Call call) return call.getAmount();
              if (a instanceof SmallBlind sb) return sb.getAmount();
              if (a instanceof BigBlind bb) return bb.getAmount();
              return 0L;
            })
        .sum();
  }

  public List<Card> getBoard() {
    return history.stream()
        .filter(a -> a instanceof RevealCards)
        .flatMap(a -> ((RevealCards) a).getCards().stream())
        .collect(Collectors.toList());
  }
}
