package com.swedapp.bank.service.account.errors;

public class InvalidExchangeException extends RuntimeException {

  public InvalidExchangeException(String message) {
    super(message);
  }
}
