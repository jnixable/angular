package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record WithdrawResponse(String accountNumber, Currency currency, BigDecimal balance, BigDecimal amount) {
}
