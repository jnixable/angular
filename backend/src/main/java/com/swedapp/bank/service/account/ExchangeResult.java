package com.swedapp.bank.service.account;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record ExchangeResult(
    String accountNumber,
    Currency fromCurrency,
    BigDecimal fromBalance,
    Currency toCurrency,
    BigDecimal toBalance,
    BigDecimal debitedAmount,
    BigDecimal creditedAmount,
    BigDecimal rate) {
}
