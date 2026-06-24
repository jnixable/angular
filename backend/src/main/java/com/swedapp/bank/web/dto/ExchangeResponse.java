package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record ExchangeResponse(
                String accountNumber,
                Currency fromCurrency,
                BigDecimal fromBalance,
                Currency toCurrency,
                BigDecimal toBalance,
                BigDecimal debitedAmount,
                BigDecimal creditedAmount,
                BigDecimal rate) {
}
