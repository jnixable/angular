package com.swedapp.bank.service.account.errors;

public class AccountAccessDeniedException extends RuntimeException {

  public AccountAccessDeniedException(String accountNumber) {
    super("Account does not belong to the current user: " + accountNumber);
  }
}
