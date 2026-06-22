package com.swedapp.bank.service.account;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record WithdrawResult(String accountNumber, Currency currency, BigDecimal balance, BigDecimal amount) {
}
