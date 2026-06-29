package vendredi.soir.karata.core.entity;

import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.rules.Rules;

@RequiredArgsConstructor
public class Dealer {
  private final Rules rules;

  public void execute(Game game, Deal deal, Action action) {
    if (!rules.isActionLegal(game, deal, action)) {
      throw new IllegalArgumentException("Illegal action");
    }

    deal.apply(action);
  }

  public void execute(Game game, Action action) {
    // Game level actions (like InitializePlayerChips)
    game.addAction(action);
  }
}
