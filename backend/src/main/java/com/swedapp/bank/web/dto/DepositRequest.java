package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

public record DepositRequest(String accountNumber, BigDecimal amount) {
}
