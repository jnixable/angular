package com.swedapp.bank.domain;

import java.util.List;

public record Account(String number, String name, String ownerCode, List<AccountBalance> balances) {
}
