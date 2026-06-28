package vendredi.soir.karata.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class Session {
  private final Table table;
  private final List<Round> rounds;

  public Session(Table table) {
    this.table = table;
    this.rounds = new ArrayList<>();
  }

  public void startNewRound(Deck deck) {
    table.getPlayers().forEach(Player::clearHand);
    Round round = new Round(table.getPlayers(), deck);
    rounds.add(round);
  }

  public Round getCurrentRound() {
    if (rounds.isEmpty()) {
      return null;
    }
    return rounds.get(rounds.size() - 1);
  }
}
