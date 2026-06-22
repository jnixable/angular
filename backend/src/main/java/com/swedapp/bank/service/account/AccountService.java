package com.swedapp.bank.service.account;

import java.math.BigDecimal;
import java.time.Instant;

import com.swedapp.bank.service.account.errors.AccountAccessDeniedException;
import com.swedapp.bank.service.account.errors.AccountNotFoundException;
import com.swedapp.bank.service.account.errors.InvalidDepositException;
import com.swedapp.bank.service.account.errors.InvalidWithdrawException;
import com.swedapp.bank.service.account.errors.WithdrawRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swedapp.bank.db.entity.TransactionEntity;
import com.swedapp.bank.db.repository.AccountRepository;
import com.swedapp.bank.db.repository.TransactionRepository;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.TransactionType;
import com.swedapp.bank.service.txchecker.TxCheckerClient;
import com.swedapp.bank.service.txchecker.TxCheckerResponse;
import com.swedapp.bank.service.txchecker.TxOperation;

@Service
public class AccountService {

  static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000");

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final TxCheckerClient txCheckerClient;

  public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
      TxCheckerClient txCheckerClient) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.txCheckerClient = txCheckerClient;
  }

  @Transactional
  public DepositResult deposit(String ownerCode, String accountNumber, BigDecimal amount) {
    validateDepositInput(accountNumber, amount);

    var account = accountRepository.findByNumber(accountNumber)
        .orElseThrow(() -> new AccountNotFoundException(accountNumber));

    if (!account.getOwnerCode().equals(ownerCode)) {
      throw new AccountAccessDeniedException(accountNumber);
    }

    var currency = account.getCurrency();
    if (currency != Currency.EUR) {
      throw new InvalidDepositException("Deposits are allowed only in EUR");
    }

    var newBalance = account.getBalance().add(amount);
    account.setBalance(newBalance);
    accountRepository.save(account);

    transactionRepository.save(new TransactionEntity(
        account.getId(), TransactionType.DEPOSIT, amount, currency, null, null, Instant.now()));

    return new DepositResult(account.getNumber(), currency, newBalance, amount);
  }

  @Transactional
  public WithdrawResult withdraw(String ownerCode, String accountNumber, BigDecimal amount) {
    validateWithdrawInput(accountNumber, amount);

    var account = accountRepository.findByNumber(accountNumber)
        .orElseThrow(() -> new AccountNotFoundException(accountNumber));

    if (!account.getOwnerCode().equals(ownerCode)) {
      throw new AccountAccessDeniedException(accountNumber);
    }

    var currency = account.getCurrency();
    if (currency != Currency.EUR) {
      throw new InvalidWithdrawException("Withdrawals are allowed only in EUR");
    }

    if (account.getBalance().compareTo(amount) < 0) {
      throw new InvalidWithdrawException("Insufficient funds");
    }

    TxCheckerResponse response = txCheckerClient.check(TxOperation.WITHDRAW, currency, amount);
    if (response == null || response.status() != TxCheckerResponse.Status.APPROVED) {
      String reason = response != null && response.message() != null
          ? response.message()
          : "Transaction was rejected by the transaction check service";
      throw new WithdrawRejectedException(reason);
    }

    var newBalance = account.getBalance().subtract(amount);
    account.setBalance(newBalance);
    accountRepository.save(account);

    transactionRepository.save(new TransactionEntity(
        account.getId(), TransactionType.WITHDRAWAL, null, null, amount, currency, Instant.now()));

    return new WithdrawResult(account.getNumber(), currency, newBalance, amount);
  }

  // of course, we can/should have much more validations (especially for account
  // number) but let's skip it for simplicity
  private static void validateDepositInput(String accountNumber, BigDecimal amount) {
    if (accountNumber == null || accountNumber.isBlank()) {
      throw new InvalidDepositException("Account number is required");
    }
    if (amount == null || amount.signum() <= 0) {
      throw new InvalidDepositException("Amount must be positive");
    }
    if (amount.compareTo(MAX_AMOUNT) > 0) {
      throw new InvalidDepositException("Amount must not exceed " + MAX_AMOUNT.toPlainString());
    }
  }

  private static void validateWithdrawInput(String accountNumber, BigDecimal amount) {
    if (accountNumber == null || accountNumber.isBlank()) {
      throw new InvalidWithdrawException("Account number is required");
    }
    if (amount == null || amount.signum() <= 0) {
      throw new InvalidWithdrawException("Amount must be positive");
    }
    if (amount.compareTo(MAX_AMOUNT) > 0) {
      throw new InvalidWithdrawException("Amount must not exceed " + MAX_AMOUNT.toPlainString());
    }
  }
}
