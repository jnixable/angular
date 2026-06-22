package com.swedapp.bank.service.account.errors;

public class DepositRejectedException extends RuntimeException {

  public DepositRejectedException(String message) {
    super(message);
  }
}
