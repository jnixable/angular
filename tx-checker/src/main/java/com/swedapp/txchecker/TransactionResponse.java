package com.swedapp.txchecker;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(Status status, String message) {

  public enum Status {
    APPROVED,
    REJECTED
  }
}
