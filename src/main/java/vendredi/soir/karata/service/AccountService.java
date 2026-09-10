package vendredi.soir.karata.service;

import java.util.NoSuchElementException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.karata.repository.model.poker.AccountEntity;
import vendredi.soir.karata.repository.poker.AccountRepository;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepository accountRepository;

  @Transactional(readOnly = true)
  public String getPhoneNumber(String username) {
    return accountRepository
        .findByUsername(username)
        .orElseThrow(() -> new NoSuchElementException("No account for user: " + username))
        .getPhoneNumber();
  }

  @Transactional
  public void setPhoneNumber(String username, String phoneNumber) {
    AccountEntity account =
        accountRepository
            .findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("No account for user: " + username));
    account.setPhoneNumber(phoneNumber);
    accountRepository.save(account);
  }
}
