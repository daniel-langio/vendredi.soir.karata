package vendredi.soir.karata.core.entity;

import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.rules.Rules;

@RequiredArgsConstructor
public class Dealer {
  private final Rules rules;

  public void execute(Deal deal, Action action) {
    if (!rules.isActionLegal(deal, action)) {
      throw new IllegalArgumentException("Illegal action");
    }

    deal.apply(action);
  }
}
