package com.swedapp.bank.service.account;

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
import com.swedapp.bank.service.account.errors.InvalidTransferException;
import com.swedapp.bank.service.account.errors.TransferRejectedException;
import com.swedapp.bank.service.txchecker.TxCheckerClient;
import com.swedapp.bank.service.txchecker.TxCheckerResponse;
import com.swedapp.bank.service.txchecker.TxOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static com.swedapp.bank.domain.Currency.EUR;
import static java.math.BigDecimal.ZERO;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final TxCheckerClient txCheckerClient;
    private final AccountLockService accountLockService;
    private final TransactionTemplate transactionTemplate;

    public TransferService(AccountRepository accountRepository, AccountBalanceRepository accountBalanceRepository,
                           TransactionRepository transactionRepository, TxCheckerClient txCheckerClient,
                           AccountLockService accountLockService, PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.txCheckerClient = txCheckerClient;
        this.accountLockService = accountLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public TransferResult transfer(String ownerCode, String sourceAccountNumber, String destinationAccountNumber,
                                   Currency currency, BigDecimal amount) {
        validateInput(sourceAccountNumber, destinationAccountNumber, currency, amount);

        // Lock both accounts in a deterministic order to avoid deadlocks between concurrent, opposite transfers.
        var firstLock = sourceAccountNumber.compareTo(destinationAccountNumber) <= 0
                ? sourceAccountNumber
                : destinationAccountNumber;
        var secondLock = firstLock.equals(sourceAccountNumber) ? destinationAccountNumber : sourceAccountNumber;

        return accountLockService.withLock(firstLock, () ->
                accountLockService.withLock(secondLock, () ->
                        transactionTemplate.execute(
                                status -> doTransfer(ownerCode, sourceAccountNumber, destinationAccountNumber, currency, amount)
                        )
                )
        );
    }

    private TransferResult doTransfer(String ownerCode, String sourceAccountNumber, String destinationAccountNumber,
                                      Currency currency, BigDecimal amount) {
        var source = ownedAccount(ownerCode, sourceAccountNumber);
        var destination = existingAccount(destinationAccountNumber);

        if (!source.getOwnerCode().equals(destination.getOwnerCode()) && currency != EUR) {
            throw new InvalidTransferException("Transfers between different users are allowed only in EUR");
        }

        var sourceBalance = accountBalanceRepository.findByAccountIdAndCurrency(source.getId(), currency)
                .orElseThrow(() -> new InvalidTransferException("Insufficient funds"));

        if (sourceBalance.getBalance().compareTo(amount) < 0) {
            throw new InvalidTransferException("Insufficient funds");
        }

        TxCheckerResponse response = txCheckerClient.check(TxOperation.WITHDRAW, currency, amount);
        if (response == null || response.status() != TxCheckerResponse.Status.APPROVED) {
            String reason = response != null && response.message() != null
                    ? response.message()
                    : "Transaction was rejected by the transaction check service";
            throw new TransferRejectedException(reason);
        }

        var destinationBalance = findOrCreateAccountBalance(destination, currency);

        var newSourceBalance = sourceBalance.getBalance().subtract(amount);
        var newDestinationBalance = destinationBalance.getBalance().add(amount);
        sourceBalance.setBalance(newSourceBalance);
        destinationBalance.setBalance(newDestinationBalance);
        accountBalanceRepository.save(sourceBalance);
        accountBalanceRepository.save(destinationBalance);

        transactionRepository.save(new TransactionEntity(
                source.getId(), destination.getId(), TransactionType.TRANSFER,
                amount, currency, amount, currency, Instant.now()));

        return new TransferResult(
                source.getNumber(), destination.getNumber(), currency, amount, newSourceBalance);
    }

    private AccountBalanceEntity findOrCreateAccountBalance(AccountEntity account, Currency currency) {
        return accountBalanceRepository.findByAccountIdAndCurrency(account.getId(), currency)
                .orElseGet(() -> new AccountBalanceEntity(account.getId(), currency, ZERO));
    }

    private AccountEntity ownedAccount(String ownerCode, String accountNumber) {
        var account = existingAccount(accountNumber);
        if (!account.getOwnerCode().equals(ownerCode)) {
            throw new AccountAccessDeniedException(accountNumber);
        }
        return account;
    }

    private AccountEntity existingAccount(String accountNumber) {
        return accountRepository.findByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private static void validateInput(String sourceAccountNumber, String destinationAccountNumber,
                                      Currency currency, BigDecimal amount) {
        if (sourceAccountNumber == null || sourceAccountNumber.isBlank()) {
            throw new InvalidTransferException("Source account number is required");
        }
        if (destinationAccountNumber == null || destinationAccountNumber.isBlank()) {
            throw new InvalidTransferException("Destination account number is required");
        }
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new InvalidTransferException("Source and destination accounts must be different");
        }
        if (currency == null) {
            throw new InvalidTransferException("Currency is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransferException("Amount must be positive");
        }
        if (amount.compareTo(AccountService.MAX_AMOUNT) > 0) {
            throw new InvalidTransferException("Amount must not exceed " + AccountService.MAX_AMOUNT.toPlainString());
        }
    }
}
