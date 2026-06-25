package com.swedapp.bank.service.account.errors;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(UUID transactionId) {
        super("No transaction with id: " + transactionId);
    }
}
