package com.swedapp.bank.web;

import com.swedapp.bank.service.account.TransactionService;
import com.swedapp.bank.service.account.errors.AccountAccessDeniedException;
import com.swedapp.bank.service.account.errors.AccountNotFoundException;
import com.swedapp.bank.web.dto.TransactionResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            Authentication authentication) {
        var currentUserCode = authentication.getName();
        var history = transactionService.getHistory(currentUserCode, accountNumber, PageRequest.of(page, size))
                .map(tx -> new TransactionResponse(
                        tx.id(), tx.type(), tx.amountIn(), tx.currencyIn(),
                        tx.amountOut(), tx.currencyOut(), tx.createdAt()));
        return new PagedModel<>(history);
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
}
