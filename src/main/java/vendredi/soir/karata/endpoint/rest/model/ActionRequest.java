package vendredi.soir.karata.endpoint.rest.model;

import java.util.List;

public record ActionRequest(String actionType, Long amount, List<String> discard) {}
