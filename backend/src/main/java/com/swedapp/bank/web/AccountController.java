package com.swedapp.bank.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.swedapp.bank.service.account.errors.AccountAccessDeniedException;
import com.swedapp.bank.service.account.errors.AccountNotFoundException;
import com.swedapp.bank.service.account.AccountService;
import com.swedapp.bank.service.account.errors.DepositRejectedException;
import com.swedapp.bank.service.account.errors.InvalidDepositException;
import com.swedapp.bank.service.account.errors.TxCheckerUnavailableException;
import com.swedapp.bank.web.dto.DepositRequest;
import com.swedapp.bank.web.dto.DepositResponse;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping("/deposit")
  public DepositResponse deposit(@RequestBody DepositRequest request, Authentication authentication) {
    var currentUserCode = authentication.getName();
    var result = accountService.deposit(currentUserCode, request.accountNumber(), request.amount());
    return new DepositResponse(
        result.accountNumber(), result.currency(), result.balance(), result.amount());
  }

  @ExceptionHandler(InvalidDepositException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleInvalidDeposit(InvalidDepositException ex) {
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

  @ExceptionHandler(DepositRejectedException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public Map<String, String> handleDepositRejected(DepositRejectedException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(TxCheckerUnavailableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public Map<String, String> handleTxCheckerUnavailable(TxCheckerUnavailableException ex) {
    return Map.of("error", ex.getMessage());
  }
}
