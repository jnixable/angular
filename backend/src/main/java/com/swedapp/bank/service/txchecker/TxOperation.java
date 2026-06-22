package com.swedapp.bank.service.txchecker;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TxOperation {

  @JsonProperty("Deposit")
  DEPOSIT,

  @JsonProperty("Withdraw")
  WITHDRAW,
}
