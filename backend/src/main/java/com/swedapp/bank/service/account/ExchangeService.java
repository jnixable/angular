package com.swedapp.bank.service.account;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.swedapp.bank.config.ExchangeProperties;
import com.swedapp.bank.db.entity.AccountBalanceEntity;
import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.db.entity.TransactionEntity;
import com.swedapp.bank.db.repository.AccountBalanceRepository;
import com.swedapp.bank.db.repository.AccountRepository;
import com.swedapp.bank.db.repository.TransactionRepository;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.TransactionType;
import com.swedapp.bank.service.account.errors.AccountAccessDeniedException;
import com.swedapp.bank.service.account.errors.AccountNotFoundException;
import com.swedapp.bank.service.account.errors.InvalidExchangeException;

import static java.math.RoundingMode.HALF_EVEN;

@Service
public class ExchangeService {

    private static final int BALANCE_SCALE = 2;
    private static final int RATE_SCALE = 6;

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeProperties exchangeProperties;
    private final AccountLockService accountLockService;
    private final TransactionTemplate transactionTemplate;

    public ExchangeService(AccountRepository accountRepository, AccountBalanceRepository accountBalanceRepository,
                           TransactionRepository transactionRepository, ExchangeProperties exchangeProperties,
                           AccountLockService accountLockService, PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeProperties = exchangeProperties;
        this.accountLockService = accountLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // assuming there is no any fees for exchange
    public ExchangeResult exchange(String ownerCode, String accountNumber, Currency fromCurrency,
                                   Currency toCurrency, BigDecimal amount) {
        validateInput(accountNumber, fromCurrency, toCurrency, amount);

        return accountLockService.withLock(accountNumber, () ->
                transactionTemplate.execute(
                        status -> doExchange(ownerCode, accountNumber, fromCurrency, toCurrency, amount)
                )
        );
    }

    private ExchangeResult doExchange(String ownerCode, String accountNumber, Currency fromCurrency,
                                      Currency toCurrency, BigDecimal amount) {
        var account = ownedAccount(ownerCode, accountNumber);

        var fromBalance = accountBalanceRepository.findByAccountIdAndCurrency(account.getId(), fromCurrency)
                .orElseThrow(() -> new InvalidExchangeException("Insufficient funds"));

        if (fromBalance.getBalance().compareTo(amount) < 0) {
            throw new InvalidExchangeException("Insufficient funds");
        }

        var toBalance = findOrCreateAccountBalance(account, toCurrency, amount);

        var rate = rate(fromCurrency, toCurrency);
        var creditedAmount = amount.multiply(rate).setScale(BALANCE_SCALE, HALF_EVEN);

        var newFromBalance = fromBalance.getBalance().subtract(amount);
        var newToBalance = toBalance.getBalance().add(creditedAmount);
        fromBalance.setBalance(newFromBalance);
        toBalance.setBalance(newToBalance);
        accountBalanceRepository.save(fromBalance);
        accountBalanceRepository.save(toBalance);

        transactionRepository.save(new TransactionEntity(
                account.getId(), TransactionType.EXCHANGE,
                creditedAmount, toCurrency, amount, fromCurrency, Instant.now()));

        return new ExchangeResult(
                account.getNumber(), fromCurrency, newFromBalance,
                toCurrency, newToBalance,
                amount, creditedAmount, rate);
    }

    private AccountBalanceEntity findOrCreateAccountBalance(AccountEntity account, Currency toCurrency, BigDecimal amount) {
        return accountBalanceRepository.findByAccountIdAndCurrency(account.getId(), toCurrency)
                .orElseGet(() -> new AccountBalanceEntity(account.getId(), toCurrency, BigDecimal.ZERO));
    }

    private AccountEntity ownedAccount(String ownerCode, String accountNumber) {
        var account = accountRepository.findByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (!account.getOwnerCode().equals(ownerCode)) {
            throw new AccountAccessDeniedException(accountNumber);
        }
        return account;
    }

    private BigDecimal rate(Currency from, Currency to) {
        var fromRate = configuredRate(from);
        var toRate = configuredRate(to);
        return toRate.divide(fromRate, RATE_SCALE, HALF_EVEN);
    }

    private BigDecimal configuredRate(Currency currency) {
        var rate = exchangeProperties.rates().get(currency);
        if (rate == null) {
            throw new InvalidExchangeException("Exchange rate is not configured for " + currency);
        }
        return rate;
    }

    private static void validateInput(String accountNumber, Currency fromCurrency, Currency toCurrency,
                                      BigDecimal amount) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidExchangeException("Account number is required");
        }
        if (fromCurrency == null) {
            throw new InvalidExchangeException("Source currency is required");
        }
        if (toCurrency == null) {
            throw new InvalidExchangeException("Target currency is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidExchangeException("Amount must be positive");
        }
        if (amount.compareTo(AccountService.MAX_AMOUNT) > 0) {
            throw new InvalidExchangeException("Amount must not exceed " + AccountService.MAX_AMOUNT.toPlainString());
        }
        if (fromCurrency == toCurrency) {
            throw new InvalidExchangeException("Source and target currencies must be different");
        }
    }
}
