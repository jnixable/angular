package com.swedapp.txchecker;

import java.math.BigDecimal;

public record TransactionRequest(Operation operation, String currency, BigDecimal amount) {
}
