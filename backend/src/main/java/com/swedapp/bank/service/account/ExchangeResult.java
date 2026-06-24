package com.swedapp.bank.service.account;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record ExchangeResult(
    String fromAccountNumber,
    Currency fromCurrency,
    BigDecimal fromBalance,
    String toAccountNumber,
    Currency toCurrency,
    BigDecimal toBalance,
    BigDecimal debitedAmount,
    BigDecimal creditedAmount,
    BigDecimal rate) {
}
