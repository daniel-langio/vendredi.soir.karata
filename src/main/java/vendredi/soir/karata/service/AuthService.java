package vendredi.soir.karata.service;

import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.karata.endpoint.rest.exception.ConflictException;
import vendredi.soir.karata.endpoint.rest.exception.UnauthorizedException;
import vendredi.soir.karata.repository.model.poker.AccountEntity;
import vendredi.soir.karata.repository.poker.AccountRepository;

@Service
public class AuthService {
  private final AccountRepository accountRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(AccountRepository accountRepository, JwtService jwtService) {
    this.accountRepository = accountRepository;
    this.jwtService = jwtService;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  @Transactional
  public String register(String username, String password) {
    if (accountRepository.findByUsername(username).isPresent()) {
      throw new ConflictException("Username is already taken");
    }
    AccountEntity account =
        AccountEntity.builder()
            .id(UUID.randomUUID())
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .build();
    accountRepository.save(account);
    return jwtService.generateToken(username);
  }

  @Transactional(readOnly = true)
  public String login(String username, String password) {
    AccountEntity account =
        accountRepository
            .findByUsername(username)
            .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
    if (!passwordEncoder.matches(password, account.getPasswordHash())) {
      throw new UnauthorizedException("Invalid username or password");
    }
    return jwtService.generateToken(username);
  }
}
