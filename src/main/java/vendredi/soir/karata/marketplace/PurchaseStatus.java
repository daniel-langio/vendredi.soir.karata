package vendredi.soir.karata.marketplace;

public enum PurchaseStatus {
  /**
   * ifay claim submitted, not yet verified. No auto-expiry - see ChipListing/MarketplaceService.
   */
  PENDING_PAYMENT,
  COMPLETED
}
