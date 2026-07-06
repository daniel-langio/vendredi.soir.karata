package vendredi.soir.karata.endpoint.rest.model;
import java.util.List;
import java.util.UUID;
public record DealState(UUID dealId, List<String> communityCards, Long pot, Phase phase, UUID activePlayerId) {}
