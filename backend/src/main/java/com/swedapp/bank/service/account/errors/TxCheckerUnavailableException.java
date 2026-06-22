package com.swedapp.bank.service.account.errors;

public class TxCheckerUnavailableException extends RuntimeException {

  public TxCheckerUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
