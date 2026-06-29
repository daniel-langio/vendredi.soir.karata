package vendredi.soir.karata.core.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import vendredi.soir.karata.core.rules.Rules;

@Getter
public class Game {
  private final List<Player> players;
  private final Dealer dealer;
  private final Rules rules;
  private final List<Deal> deals;

  public Game(List<Player> players, Rules rules) {
    this.players = new ArrayList<>(players);
    this.rules = rules;
    this.dealer = new Dealer(rules);
    this.deals = new ArrayList<>();
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
}
