package com.swedapp.bank.web.dto;

import java.math.BigDecimal;

public record WithdrawRequest(String accountNumber, BigDecimal amount) {
}
