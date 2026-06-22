package com.swedapp.bank.service.txchecker;

public record TxCheckerResponse(Status status, String message) {

  public enum Status {
    APPROVED,
    REJECTED,
  }
}
