package com.swedapp.bank.domain;

import java.math.BigDecimal;

public record AccountBalance(Account account, Currency currency, BigDecimal balance) {
}
