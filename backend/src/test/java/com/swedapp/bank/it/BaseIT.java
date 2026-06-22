package com.swedapp.bank.it;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.math.BigDecimal;

import com.swedapp.bank.config.TestcontainersConfiguration;
import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.db.entity.UserEntity;
import com.swedapp.bank.db.repository.AccountRepository;
import com.swedapp.bank.db.repository.TransactionRepository;
import com.swedapp.bank.db.repository.UserRepository;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "app.security.jwt.secret=test-secret-key-with-at-least-256-bits-for-hmac-sha256")
public abstract class BaseIT {

  protected static final String PASSWORD = "secret-pass-123";

  protected static final String ALICE_CODE = "19900101001";
  protected static final String ALICE_FIRSTNAME = "Alice";
  protected static final String ALICE_LASTNAME = "Andersson";
  protected static final String ALICE_EMAIL = "alice@swedapp.com";

  protected static final String BOB_CODE = "19900101002";
  protected static final String BOB_FIRSTNAME = "Bob";
  protected static final String BOB_LASTNAME = "Bergstrom";
  protected static final String BOB_EMAIL = "bob@swedapp.com";

  protected static final String EUR_ACCOUNT_NUMBER = "LVTEST0000000001";
  protected static final String USD_ACCOUNT_NUMBER = "LVTEST0000000002";
  protected static final BigDecimal ACCOUNT_INITIAL_BALANCE = new BigDecimal("100.00");

  @Autowired
  protected TestRestTemplate restTemplate;

  @Autowired
  protected UserRepository userRepository;

  @Autowired
  protected AccountRepository accountRepository;

  @Autowired
  protected TransactionRepository transactionRepository;

  @Autowired
  protected PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUpBaseData() {
    // Start from a clean slate (removes Liquibase-seeded demo data).
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    userRepository.deleteAll();

    seedUsers();
    seedAccounts();
  }

  protected void seedUsers() {
    userRepository.save(new UserEntity(
        UserType.Person, ALICE_CODE, ALICE_FIRSTNAME, ALICE_LASTNAME, null, null, null,
        ALICE_EMAIL, passwordEncoder.encode(PASSWORD)));
    userRepository.save(new UserEntity(
        UserType.Person, BOB_CODE, BOB_FIRSTNAME, BOB_LASTNAME, null, null, null,
        BOB_EMAIL, passwordEncoder.encode(PASSWORD)));
  }

  protected void seedAccounts() {
    accountRepository.save(new AccountEntity(
        "Alice EUR", EUR_ACCOUNT_NUMBER, Currency.EUR, ACCOUNT_INITIAL_BALANCE, ALICE_CODE));
    accountRepository.save(new AccountEntity(
        "Alice USD", USD_ACCOUNT_NUMBER, Currency.USD, ACCOUNT_INITIAL_BALANCE, ALICE_CODE));
  }

  @AfterEach
  void tearDown() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    userRepository.deleteAll();
  }
}
