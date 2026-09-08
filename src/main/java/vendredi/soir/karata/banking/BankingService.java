package vendredi.soir.karata.banking;

import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.karata.endpoint.rest.exception.BadRequestException;

/**
 * A user's persistent chip balance, independent of any table - separate from a single game's
 * in-progress stack (see PlayerEntity/Game.getChips), which only exists for the duration of that
 * game.
 */
@Service
@AllArgsConstructor
public class BankingService {
  public static final long STARTING_CHIPS = 1000L;

  private final WalletRepository walletRepository;

  @Transactional
  public void openWallet(String username) {
    walletRepository.save(
        Wallet.builder().id(UUID.randomUUID()).username(username).chips(STARTING_CHIPS).build());
  }

  @Transactional(readOnly = true)
  public long getBalance(String username) {
    return findWallet(username).getChips();
  }

  /** Debits a table buy-in from the player's wallet - rejects the join if funds are insufficient. */
  @Transactional
  public void debit(String username, long amount) {
    Wallet wallet = findWalletForUpdate(username);
    if (wallet.getChips() < amount) {
      throw new BadRequestException("Insufficient chips balance");
    }
    wallet.setChips(wallet.getChips() - amount);
    walletRepository.save(wallet);
  }

  /** Credits chips back to a player's wallet - e.g. cashing out on leave or table close. */
  @Transactional
  public void credit(String username, long amount) {
    if (amount <= 0) return;
    Wallet wallet = findWalletForUpdate(username);
    wallet.setChips(wallet.getChips() + amount);
    walletRepository.save(wallet);
  }

  private Wallet findWallet(String username) {
    return walletRepository
        .findByUsername(username)
        .orElseThrow(() -> new NoSuchElementException("No wallet for user: " + username));
  }

  private Wallet findWalletForUpdate(String username) {
    return walletRepository
        .findByUsernameForUpdate(username)
        .orElseThrow(() -> new NoSuchElementException("No wallet for user: " + username));
  }
}
