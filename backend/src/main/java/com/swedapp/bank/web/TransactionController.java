package com.swedapp.bank.web;

import com.swedapp.bank.domain.Transaction;
import com.swedapp.bank.service.account.TransactionService;
import com.swedapp.bank.service.account.errors.AccountAccessDeniedException;
import com.swedapp.bank.service.account.errors.AccountNotFoundException;
import com.swedapp.bank.service.account.errors.TransactionNotFoundException;
import com.swedapp.bank.web.dto.TransactionResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/accounts/{accountNumber}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public PagedModel<TransactionResponse> history(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Authentication authentication) {
        var currentUserCode = authentication.getName();
        var history = transactionService
                .getHistory(currentUserCode, accountNumber, from, to, PageRequest.of(page, size))
                .map(this::toResponse);
        return new PagedModel<>(history);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse transaction(
            @PathVariable String accountNumber,
            @PathVariable UUID transactionId,
            Authentication authentication) {
        var currentUserCode = authentication.getName();
        return toResponse(transactionService.getTransaction(currentUserCode, accountNumber, transactionId));
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.id(), tx.type(), tx.amountIn(), tx.currencyIn(),
                tx.amountOut(), tx.currencyOut(), tx.accountFrom(), tx.accountTo(), tx.createdAt());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public Map<String, String> handleAccountNotFound(AccountNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    @ResponseStatus(FORBIDDEN)
    public Map<String, String> handleAccessDenied(AccountAccessDeniedException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public Map<String, String> handleTransactionNotFound(TransactionNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }
}
