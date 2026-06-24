package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

public record ExchangeRequest(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
}
