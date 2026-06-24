package com.swedapp.bank.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.swedapp.bank.service.account.errors.AccountAccessDeniedException;
import com.swedapp.bank.service.account.errors.AccountNotFoundException;
import com.swedapp.bank.service.account.AccountService;
import com.swedapp.bank.service.account.ExchangeService;
import com.swedapp.bank.service.account.errors.InvalidDepositException;
import com.swedapp.bank.service.account.errors.InvalidExchangeException;
import com.swedapp.bank.service.account.errors.InvalidWithdrawException;
import com.swedapp.bank.service.account.errors.LockAcquisitionException;
import com.swedapp.bank.service.account.errors.TxCheckerUnavailableException;
import com.swedapp.bank.service.account.errors.WithdrawRejectedException;
import com.swedapp.bank.web.dto.AccountResponse;
import com.swedapp.bank.web.dto.DepositRequest;
import com.swedapp.bank.web.dto.DepositResponse;
import com.swedapp.bank.web.dto.ExchangeRequest;
import com.swedapp.bank.web.dto.ExchangeResponse;
import com.swedapp.bank.web.dto.WithdrawRequest;
import com.swedapp.bank.web.dto.WithdrawResponse;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;
  private final ExchangeService exchangeService;

  public AccountController(AccountService accountService, ExchangeService exchangeService) {
    this.accountService = accountService;
    this.exchangeService = exchangeService;
  }

  @GetMapping
  public List<AccountResponse> listAccounts(Authentication authentication) {
    var currentUserCode = authentication.getName();
    return accountService.listAccounts(currentUserCode).stream()
        .map(account -> new AccountResponse(
            account.number(), account.name(), account.currency(), account.balance()))
        .toList();
  }

  @PostMapping("/deposit")
  public DepositResponse deposit(@RequestBody DepositRequest request, Authentication authentication) {
    var currentUserCode = authentication.getName();
    var result = accountService.deposit(currentUserCode, request.accountNumber(), request.amount());
    return new DepositResponse(
        result.accountNumber(), result.currency(), result.balance(), result.amount());
  }

  @PostMapping("/withdraw")
  public WithdrawResponse withdraw(@RequestBody WithdrawRequest request, Authentication authentication) {
    var currentUserCode = authentication.getName();
    var result = accountService.withdraw(currentUserCode, request.accountNumber(), request.amount());
    return new WithdrawResponse(
        result.accountNumber(), result.currency(), result.balance(), result.amount());
  }

  @PostMapping("/exchange")
  public ExchangeResponse exchange(@RequestBody ExchangeRequest request, Authentication authentication) {
    var currentUserCode = authentication.getName();
    var result = exchangeService.exchange(
        currentUserCode, request.fromAccountNumber(), request.toAccountNumber(), request.amount());
    return new ExchangeResponse(
        result.fromAccountNumber(), result.fromCurrency(), result.fromBalance(),
        result.toAccountNumber(), result.toCurrency(), result.toBalance(),
        result.debitedAmount(), result.creditedAmount(), result.rate());
  }

  @ExceptionHandler(InvalidDepositException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleInvalidDeposit(InvalidDepositException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(InvalidWithdrawException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleInvalidWithdraw(InvalidWithdrawException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(InvalidExchangeException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleInvalidExchange(InvalidExchangeException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(AccountNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleAccountNotFound(AccountNotFoundException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(AccountAccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Map<String, String> handleAccessDenied(AccountAccessDeniedException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(WithdrawRejectedException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public Map<String, String> handleWithdrawRejected(WithdrawRejectedException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(TxCheckerUnavailableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public Map<String, String> handleTxCheckerUnavailable(TxCheckerUnavailableException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(LockAcquisitionException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public Map<String, String> handleLockAcquisition(LockAcquisitionException ex) {
    return Map.of("error", ex.getMessage());
  }
}
