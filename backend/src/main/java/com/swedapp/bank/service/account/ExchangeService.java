package com.swedapp.bank.service.account;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.swedapp.bank.config.ExchangeProperties;
import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.db.entity.TransactionEntity;
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
    private final TransactionRepository transactionRepository;
    private final ExchangeProperties exchangeProperties;
    private final AccountLockService accountLockService;
    private final TransactionTemplate transactionTemplate;

    public ExchangeService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                           ExchangeProperties exchangeProperties, AccountLockService accountLockService,
                           PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeProperties = exchangeProperties;
        this.accountLockService = accountLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // assuming there is no any fees for exchange
    public ExchangeResult exchange(String ownerCode, String fromAccountNumber, String toAccountNumber,
                                   BigDecimal amount) {
        validateInput(fromAccountNumber, toAccountNumber, amount);

        return accountLockService.withLock(fromAccountNumber, () -> transactionTemplate.execute(
                status -> doExchange(ownerCode, fromAccountNumber, toAccountNumber, amount)));
    }

    private ExchangeResult doExchange(String ownerCode, String fromAccountNumber, String toAccountNumber,
                                      BigDecimal amount) {
        var fromAccount = ownedAccount(ownerCode, fromAccountNumber);
        var toAccount = ownedAccount(ownerCode, toAccountNumber);

        var fromCurrency = fromAccount.getCurrency();
        var toCurrency = toAccount.getCurrency();
        if (fromCurrency == toCurrency) {
            throw new InvalidExchangeException("Source and target accounts must have different currencies");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InvalidExchangeException("Insufficient funds");
        }

        var rate = rate(fromCurrency, toCurrency);
        var creditedAmount = amount.multiply(rate).setScale(BALANCE_SCALE, HALF_EVEN);

        var newFromBalance = fromAccount.getBalance().subtract(amount);
        var newToBalance = toAccount.getBalance().add(creditedAmount);
        fromAccount.setBalance(newFromBalance);
        toAccount.setBalance(newToBalance);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transactionRepository.save(new TransactionEntity(
                fromAccount.getId(), TransactionType.EXCHANGE,
                creditedAmount, toCurrency, amount, fromCurrency, Instant.now()));

        return new ExchangeResult(
                fromAccount.getNumber(), fromCurrency, newFromBalance,
                toAccount.getNumber(), toCurrency, newToBalance,
                amount, creditedAmount, rate);
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

    private static void validateInput(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (fromAccountNumber == null || fromAccountNumber.isBlank()) {
            throw new InvalidExchangeException("Source account number is required");
        }
        if (toAccountNumber == null || toAccountNumber.isBlank()) {
            throw new InvalidExchangeException("Target account number is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidExchangeException("Amount must be positive");
        }
        if (amount.compareTo(AccountService.MAX_AMOUNT) > 0) {
            throw new InvalidExchangeException("Amount must not exceed " + AccountService.MAX_AMOUNT.toPlainString());
        }
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new InvalidExchangeException("Source and target accounts must be different");
        }
    }
}
