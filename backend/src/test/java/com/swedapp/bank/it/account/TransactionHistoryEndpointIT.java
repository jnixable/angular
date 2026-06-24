package com.swedapp.bank.it.account;

import com.swedapp.bank.db.entity.TransactionEntity;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.LoginResponse;
import com.swedapp.bank.web.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.swedapp.bank.domain.Currency.EUR;
import static com.swedapp.bank.domain.TransactionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

class TransactionHistoryEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";

    private static String historyUrl(String accountNumber) {
        return "/api/accounts/" + accountNumber + "/transactions";
    }

    @Test
    void returnsTransactionsNewestFirst() {
        var accountId = accountRepository.findByNumber(ALICE_ACCOUNT_NUMBER).orElseThrow().getId();
        var now = Instant.now();
        transactionRepository.save(new TransactionEntity(
                accountId, DEPOSIT, new BigDecimal("10.00"), EUR, null, null, now.minusSeconds(30)));
        transactionRepository.save(new TransactionEntity(
                accountId, WITHDRAWAL, null, null, new BigDecimal("5.00"), EUR, now.minusSeconds(20)));
        transactionRepository.save(new TransactionEntity(
                accountId, EXCHANGE, new BigDecimal("11.40"), Currency.USD, new BigDecimal("10.00"),
                EUR, now.minusSeconds(10)));

        var response = restTemplate.exchange(
                historyUrl(ALICE_ACCOUNT_NUMBER), HttpMethod.GET, authorized(ALICE_CODE), pageType());

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content())
                .extracting(TransactionResponse::type)
                .containsExactly(EXCHANGE, WITHDRAWAL, DEPOSIT);
    }

    @Test
    void paginatesResults() {
        var accountId = accountRepository.findByNumber(ALICE_ACCOUNT_NUMBER).orElseThrow().getId();
        var now = Instant.now();
        for (int i = 0; i < 3; i++) {
            transactionRepository.save(new TransactionEntity(
                    accountId, DEPOSIT, new BigDecimal("1.00"), EUR, null, null, now.minusSeconds(i)));
        }

        var firstPage = restTemplate.exchange(
                historyUrl(ALICE_ACCOUNT_NUMBER) + "?page=0&size=2", HttpMethod.GET, authorized(ALICE_CODE), pageType());
        var secondPage = restTemplate.exchange(
                historyUrl(ALICE_ACCOUNT_NUMBER) + "?page=1&size=2", HttpMethod.GET, authorized(ALICE_CODE), pageType());

        assertThat(firstPage.getStatusCode()).isEqualTo(OK);
        assertThat(firstPage.getBody().content()).hasSize(2);
        assertThat(secondPage.getBody().content()).hasSize(1);
    }

    @Test
    void onlyReturnsTransactionsForTheRequestedAccount() {
        var aliceId = accountRepository.findByNumber(ALICE_ACCOUNT_NUMBER).orElseThrow().getId();
        var bobId = accountRepository.findByNumber(BOB_ACCOUNT_NUMBER).orElseThrow().getId();
        transactionRepository.save(new TransactionEntity(
                aliceId, DEPOSIT, new BigDecimal("10.00"), EUR, null, null, Instant.now()));
        transactionRepository.save(new TransactionEntity(
                bobId, DEPOSIT, new BigDecimal("99.00"), EUR, null, null, Instant.now()));

        var response = restTemplate.exchange(
                historyUrl(ALICE_ACCOUNT_NUMBER), HttpMethod.GET, authorized(ALICE_CODE), pageType());

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).amountIn()).isEqualByComparingTo("10.00");
    }

    @Test
    void accessingAnotherUsersAccountReturnsForbidden() {
        var response = restTemplate.exchange(
                historyUrl(ALICE_ACCOUNT_NUMBER), HttpMethod.GET, authorized(BOB_CODE), String.class);

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void unknownAccountReturnsNotFound() {
        var response = restTemplate.exchange(
                historyUrl("LVTEST0000000999"), HttpMethod.GET, authorized(ALICE_CODE), String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.getForEntity(historyUrl(ALICE_ACCOUNT_NUMBER), String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    private ParameterizedTypeReference<PagedTransactions> pageType() {
        return new ParameterizedTypeReference<>() {
        };
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

    private record PagedTransactions(List<TransactionResponse> content) {
    }
}
