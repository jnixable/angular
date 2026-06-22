package com.swedapp.bank.service.account.errors;

/**
 * Thrown when a deposit request fails input validation (positive amount, EUR,
 * max limit, currency match).
 */
public class InvalidDepositException extends RuntimeException {

  public InvalidDepositException(String message) {
    super(message);
  }
}
