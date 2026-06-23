package com.swedapp.bank.it.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;

import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.AccountResponse;
import com.swedapp.bank.web.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

class ListAccountsEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";
    private static final String ACCOUNTS_URL = "/api/accounts";

    @Test
    void returnsCurrentUsersAccountsWithBalances() {
        var response = restTemplate.exchange(
                ACCOUNTS_URL, HttpMethod.GET, authorized(ALICE_CODE), AccountResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody())
                .extracting(AccountResponse::number)
                .containsExactlyInAnyOrder(EUR_ACCOUNT_NUMBER, USD_ACCOUNT_NUMBER);
        assertThat(response.getBody())
                .allSatisfy(account -> assertThat(account.balance()).isEqualByComparingTo(ACCOUNT_INITIAL_BALANCE));

        var eur = java.util.Arrays.stream(response.getBody())
                .filter(a -> a.number().equals(EUR_ACCOUNT_NUMBER))
                .findFirst().orElseThrow();
        assertThat(eur.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void returnsEmptyListWhenUserHasNoAccounts() {
        var response = restTemplate.exchange(
                ACCOUNTS_URL, HttpMethod.GET, authorized(BOB_CODE), AccountResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.getForEntity(ACCOUNTS_URL, String.class);

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
