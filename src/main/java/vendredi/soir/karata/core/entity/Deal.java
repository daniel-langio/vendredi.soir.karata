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
        .filter(a -> a instanceof DealHoleCard dhc && dhc.player().equals(player))
        .map(a -> ((DealHoleCard) a).card())
        .collect(Collectors.toList());
  }

  public boolean hasFolded(Player player) {
    return history.stream().anyMatch(a -> a instanceof Fold f && f.player().equals(player));
  }

  public boolean isAllIn(Player player, Game game) {
    return game.getChips(player) == 0;
  }

  public long getContribution(Player player) {
    return history.stream()
        .filter(a -> a instanceof PlayerAction pa && pa.player().equals(player))
        .mapToLong(
            a -> {
              if (a instanceof Bet bet) return bet.amount();
              if (a instanceof Raise raise) return raise.amount();
              if (a instanceof Call call) return call.amount();
              if (a instanceof SmallBlind sb) return sb.amount();
              if (a instanceof BigBlind bb) return bb.amount();
              return 0L;
            })
        .sum();
  }

  public long getPlayerRoundContribution(Player player) {
    List<Action> roundActions = getActionsInCurrentPhase();
    return roundActions.stream()
        .filter(a -> a instanceof PlayerAction pa && pa.player().equals(player))
        .mapToLong(
            a -> {
              if (a instanceof Bet bet) return bet.amount();
              if (a instanceof Raise raise) return raise.amount();
              if (a instanceof Call call) return call.amount();
              if (a instanceof SmallBlind sb) return sb.amount();
              if (a instanceof BigBlind bb) return bb.amount();
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
                    PlayerAction::player,
                    Collectors.summingLong(
                        a -> {
                          if (a instanceof Bet bet) return bet.amount();
                          if (a instanceof Raise raise) return raise.amount();
                          if (a instanceof Call call) return call.amount();
                          if (a instanceof SmallBlind sb) return sb.amount();
                          if (a instanceof BigBlind bb) return bb.amount();
                          return 0L;
                        })));
    return contributions.values().stream().max(Long::compare).orElse(0L);
  }

  public String getCurrentPhase() {
    long revealCount = history.stream().filter(a -> a instanceof RevealCards).count();
    if (revealCount == 0) return "PRE_FLOP";
    if (revealCount == 1) return "FLOP";
    if (revealCount == 2) return "TURN";
    if (revealCount == 3) return "RIVER";
    return "SHOWDOWN";
  }

  private List<Action> getActionsInCurrentPhase() {
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
              if (a instanceof Bet bet) return bet.amount();
              if (a instanceof Raise raise) return raise.amount();
              if (a instanceof Call call) return call.amount();
              if (a instanceof SmallBlind sb) return sb.amount();
              if (a instanceof BigBlind bb) return bb.amount();
              return 0L;
            })
        .sum();
  }

  public List<Card> getBoard() {
    return history.stream()
        .filter(a -> a instanceof RevealCards)
        .flatMap(a -> ((RevealCards) a).cards().stream())
        .collect(Collectors.toList());
  }
}
