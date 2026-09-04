package vendredi.soir.karata.endpoint.rest.model;

/**
 * Betting context personalized for the authenticated caller. Present only when the caller is a
 * seated, authenticated player in this game and a deal is in progress.
 */
public record YouState(Long callAmount, Long minRaise, Long maxRaise) {}
