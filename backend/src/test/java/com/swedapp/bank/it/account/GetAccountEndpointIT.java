package com.swedapp.bank.it.account;

import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.AccountResponse;
import com.swedapp.bank.web.dto.BalanceResponse;
import com.swedapp.bank.web.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

class GetAccountEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";

    private static String accountUrl(String accountNumber) {
        return "/api/accounts/" + accountNumber;
    }

    @Test
    void returnsAccountWithAllCurrencyBalancesForOwner() {
        var response = restTemplate.exchange(
                accountUrl(ALICE_ACCOUNT_NUMBER), HttpMethod.GET, authorized(ALICE_CODE), AccountResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().number()).isEqualTo(ALICE_ACCOUNT_NUMBER);
        assertThat(response.getBody().name()).isEqualTo("Alice Wallet");
        assertThat(response.getBody().balances())
                .extracting(BalanceResponse::currency)
                .containsExactlyInAnyOrder(Currency.EUR, Currency.USD);
        assertThat(response.getBody().balances())
                .allSatisfy(balance -> assertThat(balance.balance()).isEqualByComparingTo(ACCOUNT_INITIAL_BALANCE));
    }

    @Test
    void accessingAnotherUsersAccountReturnsForbidden() {
        var response = restTemplate.exchange(
                accountUrl(ALICE_ACCOUNT_NUMBER), HttpMethod.GET, authorized(BOB_CODE), String.class);

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void unknownAccountReturnsNotFound() {
        var response = restTemplate.exchange(
                accountUrl("LVTEST0000000999"), HttpMethod.GET, authorized(ALICE_CODE), String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.getForEntity(accountUrl(ALICE_ACCOUNT_NUMBER), String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    private HttpEntity<Void> authorized(String code) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(login(code));
        return new HttpEntity<>(headers);
    }

    private String login(String code) {
        var response = restTemplate.postForEntity(
                LOGIN_URL, Map.of("code", code, "password", PASSWORD), LoginResponse.class);
        return response.getBody().token();
    }
}
