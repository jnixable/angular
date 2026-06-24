package com.swedapp.bank.service.account;

import com.swedapp.bank.domain.Currency;

import java.math.BigDecimal;

public record TransferResult(
        String sourceAccountNumber,
        String destinationAccountNumber,
        Currency currency,
        BigDecimal amount,
        BigDecimal sourceBalance) {
}
