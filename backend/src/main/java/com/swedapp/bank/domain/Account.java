package com.swedapp.bank.domain;

import java.math.BigDecimal;

public record Account(String name, String number, Currency currency , BigDecimal balance, String ownerCode) {
}
