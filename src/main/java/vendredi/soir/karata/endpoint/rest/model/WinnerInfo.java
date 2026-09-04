package vendredi.soir.karata.endpoint.rest.model;

import java.util.UUID;

public record WinnerInfo(UUID playerId, String username, Long amount, String handRank) {}
