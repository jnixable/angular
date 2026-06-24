package com.swedapp.bank.it;

import com.swedapp.bank.config.TestcontainersConfiguration;
import com.swedapp.bank.db.entity.AccountBalanceEntity;
import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.db.entity.UserEntity;
import com.swedapp.bank.db.repository.AccountBalanceRepository;
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

import java.math.BigDecimal;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

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

    // Alice owns a multi-currency wallet (EUR + USD), Bob owns a wallet with EUR only.
    protected static final String ALICE_ACCOUNT_NUMBER = "LVTEST0000000001";
    protected static final String BOB_ACCOUNT_NUMBER = "LVTEST0000000002";
    protected static final BigDecimal ACCOUNT_INITIAL_BALANCE = new BigDecimal("100.00");

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected AccountBalanceRepository accountBalanceRepository;

    @Autowired
    protected TransactionRepository transactionRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpBaseData() {
        // Start from a clean slate (removes Liquibase-seeded demo data).
        transactionRepository.deleteAll();
        accountBalanceRepository.deleteAll();
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
        var alice = accountRepository.save(new AccountEntity("Alice Wallet", ALICE_ACCOUNT_NUMBER, ALICE_CODE));
        accountBalanceRepository.save(new AccountBalanceEntity(alice.getId(), Currency.EUR, ACCOUNT_INITIAL_BALANCE));
        accountBalanceRepository.save(new AccountBalanceEntity(alice.getId(), Currency.USD, ACCOUNT_INITIAL_BALANCE));

        var bob = accountRepository.save(new AccountEntity("Bob Wallet", BOB_ACCOUNT_NUMBER, BOB_CODE));
        accountBalanceRepository.save(new AccountBalanceEntity(bob.getId(), Currency.EUR, ACCOUNT_INITIAL_BALANCE));
    }

    protected BigDecimal balanceOf(String accountNumber, Currency currency) {
        var account = accountRepository.findByNumber(accountNumber).orElseThrow();
        return accountBalanceRepository.findByAccountIdAndCurrency(account.getId(), currency)
                .orElseThrow()
                .getBalance();
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        accountBalanceRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }
}
