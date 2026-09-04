package vendredi.soir.karata.endpoint.rest.model;

import java.util.UUID;

public record PlayerInfo(
    UUID playerId,
    String username,
    Long chips,
    PlayerStatus status,
    Long contributionThisRound,
    BlindRole blind,
    String lastAction) {}
