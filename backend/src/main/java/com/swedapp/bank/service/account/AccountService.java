package com.swedapp.bank.service.account;

import com.swedapp.bank.db.entity.AccountBalanceEntity;
import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.db.entity.TransactionEntity;
import com.swedapp.bank.db.repository.AccountBalanceRepository;
import com.swedapp.bank.db.repository.AccountRepository;
import com.swedapp.bank.db.repository.TransactionRepository;
import com.swedapp.bank.domain.Account;
import com.swedapp.bank.domain.AccountBalance;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.service.account.errors.*;
import com.swedapp.bank.service.txchecker.TxCheckerClient;
import com.swedapp.bank.service.txchecker.TxCheckerResponse;
import com.swedapp.bank.service.txchecker.TxOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.swedapp.bank.domain.TransactionType.DEPOSIT;
import static com.swedapp.bank.domain.TransactionType.WITHDRAWAL;
import static java.math.BigDecimal.ZERO;

@Service
public class AccountService {

    static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000");

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final TxCheckerClient txCheckerClient;
    private final AccountLockService accountLockService;
    private final TransactionTemplate transactionTemplate;

    public AccountService(AccountRepository accountRepository, AccountBalanceRepository accountBalanceRepository,
                          TransactionRepository transactionRepository, TxCheckerClient txCheckerClient,
                          AccountLockService accountLockService, PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.txCheckerClient = txCheckerClient;
        this.accountLockService = accountLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<AccountBalance> listAccounts(String ownerCode) {
        return accountRepository.findByOwnerCode(ownerCode).stream()
                .flatMap(account -> {
                    var domainAccount = new Account(account.getNumber(), account.getName(), account.getOwnerCode());
                    return accountBalanceRepository.findByAccountId(account.getId()).stream()
                            .map(balance -> new AccountBalance(domainAccount, balance.getCurrency(), balance.getBalance()));
                })
                .toList();
    }

    public DepositResult deposit(String ownerCode, String accountNumber, Currency currency, BigDecimal amount) {
        validateDepositInput(accountNumber, currency, amount);

        return accountLockService.withLock(accountNumber, () ->
                transactionTemplate.execute(
                        status -> doDeposit(ownerCode, accountNumber, currency, amount
                        )
                )
        );
    }

    private DepositResult doDeposit(String ownerCode, String accountNumber, Currency currency, BigDecimal amount) {
        var account = ownedAccount(ownerCode, accountNumber);

        var balance = findOrCreateAccountBalance(account, currency);

        var newBalance = balance.getBalance().add(amount);
        balance.setBalance(newBalance);
        accountBalanceRepository.save(balance);

        transactionRepository.save(new TransactionEntity(
                account.getId(), DEPOSIT, amount, currency, null, null, Instant.now()));

        return new DepositResult(account.getNumber(), currency, newBalance, amount);
    }

    public WithdrawResult withdraw(String ownerCode, String accountNumber, Currency currency, BigDecimal amount) {
        validateWithdrawInput(accountNumber, currency, amount);

        return accountLockService.withLock(accountNumber, () ->
                transactionTemplate.execute(
                        status -> doWithdraw(ownerCode, accountNumber, currency, amount)
                )
        );
    }

    private AccountBalanceEntity findOrCreateAccountBalance(AccountEntity account, Currency currency) {
        return accountBalanceRepository.findByAccountIdAndCurrency(account.getId(), currency)
                .orElseGet(() -> new AccountBalanceEntity(account.getId(), currency, ZERO));
    }

    private WithdrawResult doWithdraw(String ownerCode, String accountNumber, Currency currency, BigDecimal amount) {
        var account = ownedAccount(ownerCode, accountNumber);

        var balance = accountBalanceRepository.findByAccountIdAndCurrency(account.getId(), currency)
                .orElseThrow(() -> new InvalidWithdrawException("Insufficient funds"));

        if (balance.getBalance().compareTo(amount) < 0) {
            throw new InvalidWithdrawException("Insufficient funds");
        }

        TxCheckerResponse response = txCheckerClient.check(TxOperation.WITHDRAW, currency, amount);
        if (response == null || response.status() != TxCheckerResponse.Status.APPROVED) {
            String reason = response != null && response.message() != null
                    ? response.message()
                    : "Transaction was rejected by the transaction check service";
            throw new WithdrawRejectedException(reason);
        }

        var newBalance = balance.getBalance().subtract(amount);
        balance.setBalance(newBalance);
        accountBalanceRepository.save(balance);

        transactionRepository.save(new TransactionEntity(
                account.getId(), WITHDRAWAL, null, null, amount, currency, Instant.now()));

        return new WithdrawResult(account.getNumber(), currency, newBalance, amount);
    }

    private AccountEntity ownedAccount(String ownerCode, String accountNumber) {
        var account = accountRepository.findByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (!account.getOwnerCode().equals(ownerCode)) {
            throw new AccountAccessDeniedException(accountNumber);
        }
        return account;
    }

    // of course, we can/should have much more validations but let's skip it for simplicity
    private static void validateDepositInput(String accountNumber, Currency currency, BigDecimal amount) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidDepositException("Account number is required");
        }
        if (currency == null) {
            throw new InvalidDepositException("Currency is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidDepositException("Amount must be positive");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidDepositException("Amount must not exceed " + MAX_AMOUNT.toPlainString());
        }
        if (currency != Currency.EUR) {
            throw new InvalidDepositException("Deposits are allowed only in EUR");
        }
    }

    // of course, we can/should have much more validations but let's skip it for simplicity
    private static void validateWithdrawInput(String accountNumber, Currency currency, BigDecimal amount) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidWithdrawException("Account number is required");
        }
        if (currency == null) {
            throw new InvalidWithdrawException("Currency is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidWithdrawException("Amount must be positive");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidWithdrawException("Amount must not exceed " + MAX_AMOUNT.toPlainString());
        }
        if (currency != Currency.EUR) {
            throw new InvalidWithdrawException("Withdrawals are allowed only in EUR");
        }
    }
}
