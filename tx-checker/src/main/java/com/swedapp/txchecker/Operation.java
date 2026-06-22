package com.swedapp.txchecker;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Operation {

  @JsonProperty("Deposit")
  DEPOSIT,

  @JsonProperty("Withdraw")
  WITHDRAW
}
