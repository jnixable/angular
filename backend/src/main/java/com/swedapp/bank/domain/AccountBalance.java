package com.swedapp.bank.domain;

import java.math.BigDecimal;

public record AccountBalance(Currency currency, BigDecimal balance) {
}
