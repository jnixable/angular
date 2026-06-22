package com.swedapp.bank.service.txchecker;

import java.math.BigDecimal;

public record TxCheckerRequest(TxOperation operation, String currency, BigDecimal amount) {
}
