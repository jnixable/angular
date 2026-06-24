package com.swedapp.bank.service.account.errors;

public class LockAcquisitionException extends RuntimeException {

  public LockAcquisitionException(String message) {
    super(message);
  }

  public LockAcquisitionException(String message, Throwable cause) {
    super(message, cause);
  }
}
