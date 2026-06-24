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
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class ListAccountsEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";
    private static final String ACCOUNTS_URL = "/api/accounts";

    @Test
    void returnsCurrentUsersAccountWithAllCurrencyBalances() {
        var response = restTemplate.exchange(
                ACCOUNTS_URL, HttpMethod.GET, authorized(ALICE_CODE), AccountResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody())
                .extracting(AccountResponse::number)
                .containsExactly(ALICE_ACCOUNT_NUMBER);

        var account = response.getBody()[0];
        assertThat(account.balances())
                .extracting(BalanceResponse::currency)
                .containsExactlyInAnyOrder(Currency.EUR, Currency.USD);
        assertThat(account.balances())
                .allSatisfy(balance -> assertThat(balance.balance()).isEqualByComparingTo(ACCOUNT_INITIAL_BALANCE));
    }

    @Test
    void returnsOnlyOwnAccounts() {
        var response = restTemplate.exchange(
                ACCOUNTS_URL, HttpMethod.GET, authorized(BOB_CODE), AccountResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody())
                .extracting(AccountResponse::number)
                .containsExactly(BOB_ACCOUNT_NUMBER);

        var account = response.getBody()[0];
        assertThat(account.balances())
                .extracting(BalanceResponse::currency)
                .containsExactly(Currency.EUR);
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
