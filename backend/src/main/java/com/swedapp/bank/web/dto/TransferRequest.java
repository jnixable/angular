package com.swedapp.bank.web.dto;

import com.swedapp.bank.domain.Currency;

import java.math.BigDecimal;

public record TransferRequest(
        String sourceAccountNumber,
        String destinationAccountNumber,
        Currency currency,
        BigDecimal amount) {
}
