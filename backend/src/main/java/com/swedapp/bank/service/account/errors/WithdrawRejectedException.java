package com.swedapp.bank.service.account.errors;

public class WithdrawRejectedException extends RuntimeException {

  public WithdrawRejectedException(String message) {
    super(message);
  }
}
