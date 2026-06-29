package vendredi.soir.karata.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PlayerAction {
  private final Player player;
  private final PlayerActionType type;
  private final long amount;

  public static PlayerAction of(Player player, PlayerActionType type, long amount) {
    return new PlayerAction(player, type, amount);
  }

  public static PlayerAction of(Player player, PlayerActionType type) {
    return new PlayerAction(player, type, 0);
  }
}
