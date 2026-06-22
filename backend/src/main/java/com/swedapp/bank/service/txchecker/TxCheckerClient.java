package com.swedapp.bank.service.txchecker;

import java.math.BigDecimal;

import com.swedapp.bank.service.account.errors.TxCheckerUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.swedapp.bank.domain.Currency;

/** Client for the external tx-checker service (simulated 3rd-party system). */
@Component
public class TxCheckerClient {

  private static final String CHECK_PATH = "/api/v1/transactions/check";

  private final RestClient restClient;

  public TxCheckerClient(@Value("${app.tx-checker.base-url}") String baseUrl, RestClient.Builder builder) {
    this.restClient = builder.baseUrl(baseUrl).build();
  }

  public TxCheckerResponse check(TxOperation operation, Currency currency, BigDecimal amount) {
    try {
      return restClient.post()
          .uri(CHECK_PATH)
          .body(new TxCheckerRequest(operation, currency.name(), amount))
          .retrieve()
          .body(TxCheckerResponse.class);
    } catch (RestClientException ex) {
      throw new TxCheckerUnavailableException("Transaction check service is unavailable", ex);
    }
  }
}
