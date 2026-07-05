package vendredi.soir.karata.endpoint.rest.model;

import com.fasterxml.jackson.annotation.JsonValue;

public record Card(@JsonValue String value) {}
