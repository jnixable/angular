package com.swedapp.bank.web;

import com.swedapp.bank.domain.AccountBalance;
import com.swedapp.bank.service.account.AccountService;
import com.swedapp.bank.service.account.ExchangeService;
import com.swedapp.bank.service.account.errors.*;
import com.swedapp.bank.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.swedapp.bank.domain.Currency.EUR;
import static java.util.stream.Collectors.toList;

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
                .collect(Collectors.groupingBy(AccountBalance::account, LinkedHashMap::new, toList()))
                .entrySet().stream()
                .map(entry -> new AccountResponse(
                        entry.getKey().number(),
                        entry.getKey().name(),
                        entry.getValue().stream()
                                .map(balance -> new BalanceResponse(balance.currency(), balance.balance()))
                                .toList()))
                .toList();
    }

    @PostMapping("/deposit")
    public DepositResponse deposit(@RequestBody DepositRequest request, Authentication authentication) {
        var currentUserCode = authentication.getName();
        var result = accountService.deposit(
                currentUserCode, request.accountNumber(), EUR, request.amount());
        return new DepositResponse(
                result.accountNumber(), result.currency(), result.balance(), result.amount());
    }

    @PostMapping("/withdraw")
    public WithdrawResponse withdraw(@RequestBody WithdrawRequest request, Authentication authentication) {
        var currentUserCode = authentication.getName();
        var result = accountService.withdraw(
                currentUserCode, request.accountNumber(), EUR, request.amount());
        return new WithdrawResponse(
                result.accountNumber(), result.balance(), result.amount());
    }

    @PostMapping("/exchange")
    public ExchangeResponse exchange(@RequestBody ExchangeRequest request, Authentication authentication) {
        var currentUserCode = authentication.getName();
        var result = exchangeService.exchange(
                currentUserCode, request.accountNumber(), request.fromCurrency(), request.toCurrency(), request.amount());
        return new ExchangeResponse(
                result.accountNumber(), result.fromCurrency(), result.fromBalance(),
                result.toCurrency(), result.toBalance(),
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
