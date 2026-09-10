package vendredi.soir.karata.marketplace;

public enum ListingStatus {
  ACTIVE,
  /** A buyer has started paying (ifay claim submitted) - not yet verified. No auto-expiry. */
  PENDING_PAYMENT,
  SOLD,
  CANCELLED
}
