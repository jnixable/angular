package com.swedapp.bank.web.dto;

import com.swedapp.bank.domain.Currency;

import java.math.BigDecimal;

public record TransferResponse(
        String sourceAccountNumber,
        String destinationAccountNumber,
        Currency currency,
        BigDecimal amount,
        BigDecimal sourceBalance) {
}
