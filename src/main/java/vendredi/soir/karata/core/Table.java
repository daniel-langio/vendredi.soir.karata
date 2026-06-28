package vendredi.soir.karata.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class Table {
  private final List<Player> players;
  private final int maxSeats;

  public Table(int maxSeats) {
    this.maxSeats = maxSeats;
    this.players = new ArrayList<>();
  }

  public void join(Player player) {
    if (players.size() >= maxSeats) {
      throw new IllegalStateException("Table is full");
    }
    if (players.contains(player)) {
      throw new IllegalArgumentException("Player already at table");
    }
    players.add(player);
  }

  public void leave(Player player) {
    players.remove(player);
  }
}
