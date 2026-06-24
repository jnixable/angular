package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

public record WithdrawResponse(String accountNumber, BigDecimal balance, BigDecimal amount) {
}
