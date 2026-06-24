package com.swedapp.bank.service.account.errors;

public class TransferRejectedException extends RuntimeException {

    public TransferRejectedException(String message) {
        super(message);
    }
}
