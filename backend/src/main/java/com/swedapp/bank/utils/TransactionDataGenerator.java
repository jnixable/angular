package com.swedapp.bank.utils;

import com.swedapp.bank.config.ExchangeProperties;
import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.db.entity.TransactionEntity;
import com.swedapp.bank.db.repository.AccountRepository;
import com.swedapp.bank.db.repository.TransactionRepository;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.math.RoundingMode.HALF_EVEN;

@Component
public class TransactionDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(TransactionDataGenerator.class);

    private static final String ALICE_ACCOUNT_NUMBER = "LVHABA0000000001";
    private static final String BOB_ACCOUNT_NUMBER = "LVHABA0000000002";

    private static final int TRANSACTIONS_PER_ACCOUNT = 100;
    private static final long WINDOW_DAYS = 60; // ~ last 2 months
    private static final Currency TRANSFER_CURRENCY = Currency.EUR;
    private static final long MIN_AMOUNT_CENTS = 100; // 1.00
    private static final long MAX_AMOUNT_CENTS = 2000; // 20.00
    private static final int BALANCE_SCALE = 2;
    private static final int RATE_SCALE = 6;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeProperties exchangeProperties;

    public TransactionDataGenerator(AccountRepository accountRepository,
                                    TransactionRepository transactionRepository,
                                    ExchangeProperties exchangeProperties) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeProperties = exchangeProperties;
    }

    //@PostConstruct
    public void generate() {
        if (transactionRepository.count() > 1000) {
            log.info("Transactions already present - skipping random transaction generation.");
            return;
        }

        var alice = accountRepository.findByNumber(ALICE_ACCOUNT_NUMBER).orElse(null);
        var bob = accountRepository.findByNumber(BOB_ACCOUNT_NUMBER).orElse(null);
        if (alice == null || bob == null) {
            log.warn("Seeded accounts {} / {} not found - skipping random transaction generation.",
                    ALICE_ACCOUNT_NUMBER, BOB_ACCOUNT_NUMBER);
            return;
        }

        var currencies = Currency.values();
        var now = Instant.now();
        var windowSeconds = Duration.ofDays(WINDOW_DAYS).toSeconds();

        List<TransactionEntity> generated = new ArrayList<>(2 * TRANSACTIONS_PER_ACCOUNT);
        // Anchor each batch to one account so every row is attributed to that account.
        generated.addAll(generateForAccount(alice, bob, currencies, now, windowSeconds));
        generated.addAll(generateForAccount(bob, alice, currencies, now, windowSeconds));

        transactionRepository.saveAll(generated);
        log.info("Generated {} random transactions ({} per account) over the last {} days.",
                generated.size(), TRANSACTIONS_PER_ACCOUNT, WINDOW_DAYS);
    }

    private List<TransactionEntity> generateForAccount(AccountEntity owner, AccountEntity counterparty,
                                                       Currency[] currencies, Instant now, long windowSeconds) {
        var random = ThreadLocalRandom.current();
        List<TransactionEntity> result = new ArrayList<>(TRANSACTIONS_PER_ACCOUNT);
        for (int i = 0; i < TRANSACTIONS_PER_ACCOUNT; i++) {
            var createdAt = now.minusSeconds(random.nextLong(windowSeconds));
            if (random.nextBoolean()) {
                // Outgoing transfer from the owner; cross-user transfers are EUR-only.
                result.add(transfer(owner, counterparty, createdAt));
            } else {
                result.add(randomExchange(owner, currencies, createdAt));
            }
        }
        return result;
    }

    private TransactionEntity transfer(AccountEntity source, AccountEntity destination, Instant createdAt) {
        var amount = randomAmount();
        return new TransactionEntity(
                source.getId(), destination.getId(), TransactionType.TRANSFER,
                amount, TRANSFER_CURRENCY, amount, TRANSFER_CURRENCY, createdAt);
    }

    private TransactionEntity randomExchange(AccountEntity account, Currency[] currencies, Instant createdAt) {
        var random = ThreadLocalRandom.current();
        var fromCurrency = currencies[random.nextInt(currencies.length)];
        Currency toCurrency;
        do {
            toCurrency = currencies[random.nextInt(currencies.length)];
        } while (toCurrency == fromCurrency);

        var amount = randomAmount();
        var rate = rate(fromCurrency, toCurrency);
        var creditedAmount = amount.multiply(rate).setScale(BALANCE_SCALE, HALF_EVEN);

        return new TransactionEntity(
                account.getId(), TransactionType.EXCHANGE,
                creditedAmount, toCurrency, amount, fromCurrency, createdAt);
    }

    private BigDecimal randomAmount() {
        var cents = ThreadLocalRandom.current().nextLong(MIN_AMOUNT_CENTS, MAX_AMOUNT_CENTS + 1);
        return BigDecimal.valueOf(cents, BALANCE_SCALE);
    }

    private BigDecimal rate(Currency from, Currency to) {
        var fromRate = configuredRate(from);
        var toRate = configuredRate(to);
        return toRate.divide(fromRate, RATE_SCALE, HALF_EVEN);
    }

    private BigDecimal configuredRate(Currency currency) {
        var rate = exchangeProperties.rates().get(currency);
        if (rate == null) {
            throw new IllegalStateException("Exchange rate is not configured for " + currency);
        }
        return rate;
    }
}
