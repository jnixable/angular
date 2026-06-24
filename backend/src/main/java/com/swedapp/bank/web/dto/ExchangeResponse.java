package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record ExchangeResponse(
        String fromAccountNumber,
        Currency fromCurrency,
        BigDecimal fromBalance,
        String toAccountNumber,
        Currency toCurrency,
        BigDecimal toBalance,
        BigDecimal debitedAmount,
        BigDecimal creditedAmount,
        BigDecimal rate
) {
}
