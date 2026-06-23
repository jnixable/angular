package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

import com.swedapp.bank.domain.Currency;

public record AccountResponse(String number, String name, Currency currency, BigDecimal balance) {
}
