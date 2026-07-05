package vendredi.soir.karata.endpoint.rest.model;

import java.time.Instant;

public record ActionRequest(String actionType, Long amount, Instant timeoutLimit) {}
