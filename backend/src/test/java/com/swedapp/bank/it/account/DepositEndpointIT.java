package com.swedapp.bank.it.account;

import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.DepositResponse;
import com.swedapp.bank.web.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static com.swedapp.bank.domain.Currency.EUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

class DepositEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";
    private static final String DEPOSIT_URL = "/api/accounts/deposit";

    @Test
    void depositIncreasesBalanceAndRecordsTransaction() {
        var response = restTemplate.postForEntity(
                DEPOSIT_URL,
                authorized(Map.of("accountNumber", ALICE_ACCOUNT_NUMBER, "currency", "EUR", "amount", "50.00"), ALICE_CODE),
                DepositResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accountNumber()).isEqualTo(ALICE_ACCOUNT_NUMBER);
        assertThat(response.getBody().currency()).isEqualTo(EUR);
        assertThat(response.getBody().balance()).isEqualByComparingTo("150.00");
        assertThat(response.getBody().amount()).isEqualByComparingTo("50.00");

        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, EUR)).isEqualByComparingTo("150.00");
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void nonPositiveAmountReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                DEPOSIT_URL,
                authorized(Map.of("accountNumber", ALICE_ACCOUNT_NUMBER, "currency", "EUR", "amount", "-10.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void nonEurCurrencyReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                DEPOSIT_URL,
                authorized(Map.of("accountNumber", ALICE_ACCOUNT_NUMBER, "currency", "USD", "amount", "50.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void amountAboveMaxReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                DEPOSIT_URL,
                authorized(Map.of("accountNumber", ALICE_ACCOUNT_NUMBER, "currency", "EUR", "amount", "100000001"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void unknownAccountReturnsNotFound() {
        var response = restTemplate.postForEntity(
                DEPOSIT_URL,
                authorized(Map.of("accountNumber", "SE0000000000", "currency", "EUR", "amount", "50.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void depositToForeignAccountReturnsForbidden() {
        var response = restTemplate.postForEntity(
                DEPOSIT_URL,
                authorized(Map.of("accountNumber", ALICE_ACCOUNT_NUMBER, "currency", "EUR", "amount", "50.00"), BOB_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.postForEntity(
                DEPOSIT_URL,
                Map.of("accountNumber", ALICE_ACCOUNT_NUMBER, "currency", "EUR", "amount", "50.00"),
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
