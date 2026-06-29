package vendredi.soir.karata.core;

import lombok.RequiredArgsConstructor;

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
