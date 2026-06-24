package com.swedapp.bank.it.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.math.BigDecimal;
import java.util.Map;

import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.TransactionType;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.ExchangeResponse;
import com.swedapp.bank.web.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

class ExchangeEndpointIT extends BaseIT {

  private static final String LOGIN_URL = "/api/user/login";
  private static final String EXCHANGE_URL = "/api/accounts/exchange";

  @Test
  void exchangeConvertsBetweenAccountsAndRecordsTransaction() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", USD_ACCOUNT_NUMBER,
            "amount", "10.00"), ALICE_CODE),
        ExchangeResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.fromAccountNumber()).isEqualTo(EUR_ACCOUNT_NUMBER);
    assertThat(body.fromCurrency()).isEqualTo(Currency.EUR);
    assertThat(body.fromBalance()).isEqualByComparingTo("90.00");
    assertThat(body.toAccountNumber()).isEqualTo(USD_ACCOUNT_NUMBER);
    assertThat(body.toCurrency()).isEqualTo(Currency.USD);
    assertThat(body.toBalance()).isEqualByComparingTo("111.40");
    assertThat(body.debitedAmount()).isEqualByComparingTo("10.00");
    assertThat(body.creditedAmount()).isEqualByComparingTo("11.40");
    assertThat(body.rate()).isEqualByComparingTo("1.14");

    assertThat(accountRepository.findByNumber(EUR_ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("90.00");
    assertThat(accountRepository.findByNumber(USD_ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("111.40");

    var transactions = transactionRepository.findAll();
    assertThat(transactions).hasSize(1);
    var transaction = transactions.getFirst();
    assertThat(transaction.getType()).isEqualTo(TransactionType.EXCHANGE);
    assertThat(transaction.getAmountOut()).isEqualByComparingTo("10.00");
    assertThat(transaction.getCurrencyOut()).isEqualTo(Currency.EUR);
    assertThat(transaction.getAmountIn()).isEqualByComparingTo("11.40");
    assertThat(transaction.getCurrencyIn()).isEqualTo(Currency.USD);
  }

  @Test
  void exchangeConvertsUsdToGbp() {
    accountRepository.save(new AccountEntity(
        "Alice GBP", "LVTEST0000000004", Currency.GBP, new BigDecimal("0.00"), ALICE_CODE));

    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", USD_ACCOUNT_NUMBER,
            "toAccountNumber", "LVTEST0000000004",
            "amount", "50.00"), ALICE_CODE),
        ExchangeResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.fromAccountNumber()).isEqualTo(USD_ACCOUNT_NUMBER);
    assertThat(body.fromCurrency()).isEqualTo(Currency.USD);
    assertThat(body.fromBalance()).isEqualByComparingTo("50.00");
    assertThat(body.toAccountNumber()).isEqualTo("LVTEST0000000004");
    assertThat(body.toCurrency()).isEqualTo(Currency.GBP);
    assertThat(body.toBalance()).isEqualByComparingTo("37.28");
    assertThat(body.debitedAmount()).isEqualByComparingTo("50.00");
    assertThat(body.creditedAmount()).isEqualByComparingTo("37.28");
    assertThat(body.rate()).isEqualByComparingTo("0.745614");

    assertThat(accountRepository.findByNumber(USD_ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("50.00");
    assertThat(accountRepository.findByNumber("LVTEST0000000004").orElseThrow().getBalance())
        .isEqualByComparingTo("37.28");

    var transactions = transactionRepository.findAll();
    assertThat(transactions).hasSize(1);
    var transaction = transactions.getFirst();
    assertThat(transaction.getType()).isEqualTo(TransactionType.EXCHANGE);
    assertThat(transaction.getAmountOut()).isEqualByComparingTo("50.00");
    assertThat(transaction.getCurrencyOut()).isEqualTo(Currency.USD);
    assertThat(transaction.getAmountIn()).isEqualByComparingTo("37.28");
    assertThat(transaction.getCurrencyIn()).isEqualTo(Currency.GBP);
  }

  @Test
  void sameAccountReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", EUR_ACCOUNT_NUMBER,
            "amount", "10.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void sameCurrencyReturnsBadRequest() {
    accountRepository.save(new AccountEntity(
        "Alice EUR 2", "LVTEST0000000003", Currency.EUR, new BigDecimal("100.00"), ALICE_CODE));

    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", "LVTEST0000000003",
            "amount", "10.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void insufficientFundsReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", USD_ACCOUNT_NUMBER,
            "amount", "1000.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    assertThat(accountRepository.findByNumber(EUR_ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("100.00");
    assertThat(transactionRepository.findAll()).isEmpty();
  }

  @Test
  void nonPositiveAmountReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", USD_ACCOUNT_NUMBER,
            "amount", "-10.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void amountAboveMaxReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", USD_ACCOUNT_NUMBER,
            "amount", "100000001"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void unknownSourceAccountReturnsNotFound() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", "SE0000000000",
            "toAccountNumber", USD_ACCOUNT_NUMBER,
            "amount", "10.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  void unknownTargetAccountReturnsNotFound() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", "SE0000000000",
            "amount", "10.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  void foreignAccountReturnsForbidden() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        authorized(Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", USD_ACCOUNT_NUMBER,
            "amount", "10.00"), BOB_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
  }

  @Test
  void missingTokenReturnsUnauthorized() {
    var response = restTemplate.postForEntity(
        EXCHANGE_URL,
        Map.of(
            "fromAccountNumber", EUR_ACCOUNT_NUMBER,
            "toAccountNumber", USD_ACCOUNT_NUMBER,
            "amount", "10.00"),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
  }

  private HttpEntity<Map<String, String>> authorized(Map<String, String> body, String code) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(login(code));
    return new HttpEntity<>(body, headers);
  }

  private String login(String code) {
    var response = restTemplate.postForEntity(
        LOGIN_URL, Map.of("code", code, "password", PASSWORD), LoginResponse.class);
    return response.getBody().token();
  }
}
