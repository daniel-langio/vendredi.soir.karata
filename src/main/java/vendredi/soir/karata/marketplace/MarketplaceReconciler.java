package vendredi.soir.karata.marketplace;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Covers the case where a buyer's payment gets verified but they never reopen the app to trigger
 * the lazy check-on-poll path - sweeps every still-pending purchase periodically instead. Calls
 * {@link MarketplaceService#checkAndComplete} as a genuine cross-bean call (not `this.`) so its
 * {@code @Transactional} actually applies.
 */
@Slf4j
@Component
@AllArgsConstructor
class MarketplaceReconciler {
  private final MarketplaceService marketplaceService;

  @Scheduled(fixedDelay = 60_000)
  void reconcilePendingPurchases() {
    for (ChipListing listing : marketplaceService.findAllPending()) {
      try {
        marketplaceService.checkAndComplete(listing.getId());
      } catch (Exception e) {
        log.warn("Failed to reconcile listing {}", listing.getId(), e);
      }
    }
  }
}
