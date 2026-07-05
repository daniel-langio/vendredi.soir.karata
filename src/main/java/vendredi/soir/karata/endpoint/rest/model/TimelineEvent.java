package vendredi.soir.karata.endpoint.rest.model;

import java.time.Instant;
import java.util.Map;

public record TimelineEvent(String type, Instant timestamp, Map<String, Object> payload) {}
