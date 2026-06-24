package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record ExchangeRequest(String accountNumber, Currency fromCurrency, Currency toCurrency, BigDecimal amount) {
}
