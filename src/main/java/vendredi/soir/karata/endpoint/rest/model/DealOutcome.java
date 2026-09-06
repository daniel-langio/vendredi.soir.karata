package vendredi.soir.karata.endpoint.rest.model;

import java.util.List;

public record DealOutcome(List<WinnerInfo> winners, List<RevealedHand> revealedHands) {}
