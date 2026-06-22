package com.swedapp.bank.service.account.errors;

public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(String accountNumber) {
    super("No account with number: " + accountNumber);
  }
}
