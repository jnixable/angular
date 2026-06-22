package com.swedapp.bank.it.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.math.BigDecimal;
import java.util.Map;

import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.service.account.errors.TxCheckerUnavailableException;
import com.swedapp.bank.service.txchecker.TxCheckerClient;
import com.swedapp.bank.service.txchecker.TxCheckerResponse;
import com.swedapp.bank.web.dto.DepositResponse;
import com.swedapp.bank.web.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DepositEndpointIT extends BaseIT {

  private static final String ACCOUNT_NUMBER = "LVTEST0000000001";
  private static final String USD_ACCOUNT_NUMBER = "LVTEST0000000002";
  private static final String LOGIN_URL = "/api/user/login";
  private static final String DEPOSIT_URL = "/api/accounts/deposit";

  @MockitoBean
  private TxCheckerClient txCheckerClient;

  @BeforeEach
  void seedData() {
    accountRepository.save(new AccountEntity(
        "Alice EUR", ACCOUNT_NUMBER, Currency.EUR, new BigDecimal("100.00"), ALICE_CODE));
    accountRepository.save(new AccountEntity(
        "Alice USD", USD_ACCOUNT_NUMBER, Currency.USD, new BigDecimal("100.00"), ALICE_CODE));

    when(txCheckerClient.check(any(), any(), any()))
        .thenReturn(new TxCheckerResponse(TxCheckerResponse.Status.APPROVED, null));
  }

  @Test
  void approvedDepositIncreasesBalanceAndRecordsTransaction() {
    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        authorized(Map.of("accountNumber", ACCOUNT_NUMBER, "amount", "50.00"), ALICE_CODE),
        DepositResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().accountNumber()).isEqualTo(ACCOUNT_NUMBER);
    assertThat(response.getBody().currency()).isEqualTo(Currency.EUR);
    assertThat(response.getBody().balance()).isEqualByComparingTo("150.00");
    assertThat(response.getBody().amount()).isEqualByComparingTo("50.00");

    var account = accountRepository.findByNumber(ACCOUNT_NUMBER).orElseThrow();
    assertThat(account.getBalance()).isEqualByComparingTo("150.00");
    assertThat(transactionRepository.findAll()).hasSize(1);
  }

  @Test
  void rejectedDepositReturnsUnprocessableEntityAndKeepsBalance() {
    when(txCheckerClient.check(any(), any(), any()))
        .thenReturn(new TxCheckerResponse(TxCheckerResponse.Status.REJECTED, "Magic is not allowed here"));

    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        authorized(Map.of("accountNumber", ACCOUNT_NUMBER, "amount", "51.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    assertThat(accountRepository.findByNumber(ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("100.00");
    assertThat(transactionRepository.findAll()).isEmpty();
  }

  @Test
  void txCheckerUnavailableReturnsServiceUnavailable() {
    when(txCheckerClient.check(any(), any(), any()))
        .thenThrow(new TxCheckerUnavailableException("down", new RuntimeException()));

    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        authorized(Map.of("accountNumber", ACCOUNT_NUMBER, "amount", "50.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(SERVICE_UNAVAILABLE);
    assertThat(accountRepository.findByNumber(ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("100.00");
  }

  @Test
  void nonPositiveAmountReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        authorized(Map.of("accountNumber", ACCOUNT_NUMBER, "amount", "-10.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void nonEurCurrencyReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        authorized(Map.of("accountNumber", USD_ACCOUNT_NUMBER, "amount", "50.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void amountAboveMaxReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        authorized(Map.of("accountNumber", ACCOUNT_NUMBER, "amount", "100000001"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void unknownAccountReturnsNotFound() {
    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        authorized(Map.of("accountNumber", "SE0000000000", "amount", "50.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  void depositToForeignAccountReturnsForbidden() {
    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        authorized(Map.of("accountNumber", ACCOUNT_NUMBER, "amount", "50.00"), BOB_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
  }

  @Test
  void missingTokenReturnsUnauthorized() {
    var response = restTemplate.postForEntity(
        DEPOSIT_URL,
        Map.of("accountNumber", ACCOUNT_NUMBER, "amount", "50.00"),
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
