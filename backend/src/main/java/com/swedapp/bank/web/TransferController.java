package com.swedapp.bank.web;

import com.swedapp.bank.service.account.TransferService;
import com.swedapp.bank.service.account.errors.*;
import com.swedapp.bank.web.dto.TransferRequest;
import com.swedapp.bank.web.dto.TransferResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts/transfer")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransferResponse transfer(@RequestBody TransferRequest request, Authentication authentication) {
        var currentUserCode = authentication.getName();
        var result = transferService.transfer(
                currentUserCode, request.sourceAccountNumber(), request.destinationAccountNumber(),
                request.currency(), request.amount());
        return new TransferResponse(
                result.sourceAccountNumber(), result.destinationAccountNumber(),
                result.currency(), result.amount(), result.sourceBalance());
    }

    @ExceptionHandler(InvalidTransferException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidTransfer(InvalidTransferException ex) {
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

    @ExceptionHandler(TransferRejectedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleTransferRejected(TransferRejectedException ex) {
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
