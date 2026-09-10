package vendredi.soir.karata.marketplace;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.karata.banking.BankingService;
import vendredi.soir.karata.endpoint.rest.exception.BadRequestException;
import vendredi.soir.karata.endpoint.rest.exception.ForbiddenException;
import vendredi.soir.karata.endpoint.rest.exception.NotFoundException;

/**
 * A listing escrows its chips out of the seller's wallet immediately on creation ({@link
 * BankingService#debit}, which already rejects insufficient funds) rather than checking the seller
 * still has them at sale time - refunded on cancellation, credited to the buyer on a verified sale.
 * Only usernames in {@code marketplace.seller-allowlist} can create a listing for now (config, not
 * code, so opening this up to real users later is a config change).
 */
@Service
public class MarketplaceService {
  private final ChipListingRepository chipListingRepository;
  private final BankingService bankingService;
  private final IfayClient ifayClient;
  private final Set<String> sellerAllowlist;

  public MarketplaceService(
      ChipListingRepository chipListingRepository,
      BankingService bankingService,
      IfayClient ifayClient,
      @Value("${marketplace.seller-allowlist:dev}") String sellerAllowlistCsv) {
    this.chipListingRepository = chipListingRepository;
    this.bankingService = bankingService;
    this.ifayClient = ifayClient;
    this.sellerAllowlist =
        Arrays.stream(sellerAllowlistCsv.split(",")).map(String::trim).collect(Collectors.toSet());
  }

  @Transactional
  public ChipListing createListing(
      String sellerUsername,
      long chipsAmount,
      long priceAr,
      String receivingPhoneNumber,
      PaymentProvider provider) {
    if (!sellerAllowlist.contains(sellerUsername)) {
      throw new ForbiddenException("Only approved sellers can create a listing");
    }
    if (chipsAmount <= 0) {
      throw new BadRequestException("chipsAmount must be strictly positive");
    }
    if (priceAr <= 0) {
      throw new BadRequestException("priceAr must be strictly positive");
    }

    bankingService.debit(sellerUsername, chipsAmount);

    ChipListing listing =
        ChipListing.builder()
            .id(UUID.randomUUID())
            .sellerUsername(sellerUsername)
            .chipsAmount(chipsAmount)
            .priceAr(priceAr)
            .receivingPhoneNumber(receivingPhoneNumber)
            .provider(provider)
            .status(ListingStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
    return chipListingRepository.save(listing);
  }

  @Transactional(readOnly = true)
  public List<ChipListing> listActive() {
    return chipListingRepository.findByStatus(ListingStatus.ACTIVE);
  }

  @Transactional
  public ChipListing cancelListing(String sellerUsername, UUID listingId) {
    ChipListing listing = findForUpdate(listingId);
    if (!listing.getSellerUsername().equals(sellerUsername)) {
      throw new ForbiddenException("Not your listing");
    }
    if (listing.getStatus() != ListingStatus.ACTIVE) {
      throw new BadRequestException("Only an active listing can be cancelled");
    }
    bankingService.credit(sellerUsername, listing.getChipsAmount());
    listing.setStatus(ListingStatus.CANCELLED);
    return chipListingRepository.save(listing);
  }

  @Transactional
  public ChipListing initiatePurchase(
      String buyerUsername, UUID listingId, String buyerPhoneNumber, String pspRef) {
    ChipListing listing = findForUpdate(listingId);
    if (listing.getStatus() != ListingStatus.ACTIVE) {
      throw new BadRequestException("Listing is no longer available");
    }
    if (listing.getSellerUsername().equals(buyerUsername)) {
      throw new BadRequestException("Cannot buy your own listing");
    }

    IfayClient.ClaimResponse claim =
        ifayClient.submitClaim(
            buyerPhoneNumber,
            listing.getReceivingPhoneNumber(),
            listing.getPriceAr(),
            listing.getProvider(),
            pspRef);

    listing.setStatus(ListingStatus.PENDING_PAYMENT);
    listing.setBuyerUsername(buyerUsername);
    listing.setIfayPaymentId(claim.id());
    return chipListingRepository.save(listing);
  }

  /**
   * Re-checks a pending purchase against ifay; credits the buyer and completes the sale once
   * verified.
   */
  @Transactional
  public ChipListing checkAndComplete(UUID listingId) {
    ChipListing listing = findForUpdate(listingId);
    if (listing.getStatus() != ListingStatus.PENDING_PAYMENT) {
      return listing;
    }
    IfayClient.ClaimResponse claim = ifayClient.getClaim(listing.getIfayPaymentId());
    if ("VERIFIED".equals(claim.status())) {
      bankingService.credit(listing.getBuyerUsername(), listing.getChipsAmount());
      listing.setStatus(ListingStatus.SOLD);
      listing.setSoldAt(Instant.now());
      chipListingRepository.save(listing);
    }
    return listing;
  }

  List<ChipListing> findAllPending() {
    return chipListingRepository.findByStatus(ListingStatus.PENDING_PAYMENT);
  }

  private ChipListing findForUpdate(UUID listingId) {
    return chipListingRepository
        .findByIdForUpdate(listingId)
        .orElseThrow(() -> new NotFoundException("No listing " + listingId));
  }
}
