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

import java.util.Map;

import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.service.account.errors.TxCheckerUnavailableException;
import com.swedapp.bank.service.txchecker.TxCheckerClient;
import com.swedapp.bank.service.txchecker.TxCheckerResponse;
import com.swedapp.bank.web.dto.LoginResponse;
import com.swedapp.bank.web.dto.WithdrawResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class WithdrawEndpointIT extends BaseIT {

  private static final String LOGIN_URL = "/api/user/login";
  private static final String WITHDRAW_URL = "/api/accounts/withdraw";

  @MockitoBean
  private TxCheckerClient txCheckerClient;

  @BeforeEach
  void stubTxChecker() {
    when(txCheckerClient.check(any(), any(), any()))
        .thenReturn(new TxCheckerResponse(TxCheckerResponse.Status.APPROVED, null));
  }

  @Test
  void approvedWithdrawDecreasesBalanceAndRecordsTransaction() {
    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", EUR_ACCOUNT_NUMBER, "amount", "40.00"), ALICE_CODE),
        WithdrawResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().accountNumber()).isEqualTo(EUR_ACCOUNT_NUMBER);
    assertThat(response.getBody().currency()).isEqualTo(Currency.EUR);
    assertThat(response.getBody().balance()).isEqualByComparingTo("60.00");
    assertThat(response.getBody().amount()).isEqualByComparingTo("40.00");

    var account = accountRepository.findByNumber(EUR_ACCOUNT_NUMBER).orElseThrow();
    assertThat(account.getBalance()).isEqualByComparingTo("60.00");
    assertThat(transactionRepository.findAll()).hasSize(1);
  }

  @Test
  void rejectedWithdrawReturnsUnprocessableEntityAndKeepsBalance() {
    when(txCheckerClient.check(any(), any(), any()))
        .thenReturn(new TxCheckerResponse(TxCheckerResponse.Status.REJECTED, "Magic is not allowed here"));

    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", EUR_ACCOUNT_NUMBER, "amount", "41.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    assertThat(accountRepository.findByNumber(EUR_ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("100.00");
    assertThat(transactionRepository.findAll()).isEmpty();
  }

  @Test
  void txCheckerUnavailableReturnsServiceUnavailable() {
    when(txCheckerClient.check(any(), any(), any()))
        .thenThrow(new TxCheckerUnavailableException("down", new RuntimeException()));

    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", EUR_ACCOUNT_NUMBER, "amount", "40.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(SERVICE_UNAVAILABLE);
    assertThat(accountRepository.findByNumber(EUR_ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("100.00");
  }

  @Test
  void insufficientFundsReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", EUR_ACCOUNT_NUMBER, "amount", "150.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    assertThat(accountRepository.findByNumber(EUR_ACCOUNT_NUMBER).orElseThrow().getBalance())
        .isEqualByComparingTo("100.00");
    assertThat(transactionRepository.findAll()).isEmpty();
  }

  @Test
  void nonPositiveAmountReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", EUR_ACCOUNT_NUMBER, "amount", "-10.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void nonEurCurrencyReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", USD_ACCOUNT_NUMBER, "amount", "50.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void amountAboveMaxReturnsBadRequest() {
    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", EUR_ACCOUNT_NUMBER, "amount", "100000001"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void unknownAccountReturnsNotFound() {
    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", "SE0000000000", "amount", "50.00"), ALICE_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  void withdrawFromForeignAccountReturnsForbidden() {
    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        authorized(Map.of("accountNumber", EUR_ACCOUNT_NUMBER, "amount", "40.00"), BOB_CODE),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
  }

  @Test
  void missingTokenReturnsUnauthorized() {
    var response = restTemplate.postForEntity(
        WITHDRAW_URL,
        Map.of("accountNumber", EUR_ACCOUNT_NUMBER, "amount", "40.00"),
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
