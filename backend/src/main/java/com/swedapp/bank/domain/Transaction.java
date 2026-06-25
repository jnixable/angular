package com.swedapp.bank.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
                UUID id,
                TransactionType type,
                BigDecimal amountIn,
                Currency currencyIn,
                BigDecimal amountOut,
                Currency currencyOut,
                String accountFrom,
                String accountTo,
                Instant createdAt) {
}
