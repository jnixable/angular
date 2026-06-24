package com.swedapp.bank.web.dto;

import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amountIn,
        Currency currencyIn,
        BigDecimal amountOut,
        Currency currencyOut,
        Instant createdAt) {
}
