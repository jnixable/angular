package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record BalanceResponse(Currency currency, BigDecimal balance) {
}
