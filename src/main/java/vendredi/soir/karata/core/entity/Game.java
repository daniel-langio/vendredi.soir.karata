package vendredi.soir.karata.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import vendredi.soir.karata.core.action.*;
import vendredi.soir.karata.core.rules.Rules;

@Getter
public class Game {
  private final List<Player> players;
  private final Dealer dealer;
  private final Rules rules;
  private final List<Deal> deals;
  private final List<Action> history;
  @Setter private UUID currentDealId;

  public Game(List<Player> players, Rules rules) {
    this.players = new ArrayList<>(players);
    this.rules = rules;
    this.dealer = new Dealer(rules);
    this.deals = new ArrayList<>();
    this.history = new ArrayList<>();
  }

  public void addAction(Action action) {
    this.history.add(action);
  }

  public Deal startNewDeal(Deck deck) {
    Deal deal = new Deal(deck);
    deals.add(deal);
    return deal;
  }

  public Deal getCurrentDeal() {
    if (deals.isEmpty()) {
      return null;
    }
    return deals.get(deals.size() - 1);
  }

  public long getChips(Player player) {
    long chips = 0;
    chips += history.stream()
            .filter(a -> a instanceof InitializePlayerChips ipc && ipc.getPlayer().equals(player))
            .mapToLong(a -> ((InitializePlayerChips) a).getAmount())
            .sum();
    for (Deal deal : deals) {
      chips += deal.getHistory().stream()
              .mapToLong(a -> {
                if (a instanceof PlayerAction pa && pa.getPlayer().equals(player)) {
                  if (a instanceof Bet bet) return -bet.getAmount();
                  if (a instanceof Raise raise) return -raise.getAmount();
                  if (a instanceof Call call) return -call.getAmount();
                  if (a instanceof SmallBlind sb) return -sb.getAmount();
                  if (a instanceof BigBlind bb) return -bb.getAmount();
                }
                if (a instanceof AwardPot ap && ap.getWinner().equals(player)) {
                  return ap.getAmount();
                }
                return 0L;
              })
              .sum();
    }
    return chips;
  }
}
