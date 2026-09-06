package vendredi.soir.karata.endpoint.rest.model;

import java.util.List;
import java.util.UUID;

public record RevealedHand(
    UUID playerId, String username, List<String> holeCards, String handRank) {}
