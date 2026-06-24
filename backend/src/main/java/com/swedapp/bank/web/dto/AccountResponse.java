package com.swedapp.bank.web.dto;

import java.util.List;

public record AccountResponse(String number, String name, List<BalanceResponse> balances) {
}
