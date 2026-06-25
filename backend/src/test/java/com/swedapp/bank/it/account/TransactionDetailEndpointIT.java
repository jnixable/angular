package com.swedapp.bank.it.account;

import com.swedapp.bank.db.entity.TransactionEntity;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.LoginResponse;
import com.swedapp.bank.web.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.swedapp.bank.domain.Currency.EUR;
import static com.swedapp.bank.domain.TransactionType.DEPOSIT;
import static com.swedapp.bank.domain.TransactionType.TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

class TransactionDetailEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";

    private static String transactionUrl(String accountNumber, Object transactionId) {
        return "/api/accounts/" + accountNumber + "/transactions/" + transactionId;
    }

    @Test
    void returnsTransactionForOwner() {
        var accountId = accountRepository.findByNumber(ALICE_ACCOUNT_NUMBER).orElseThrow().getId();
        var saved = transactionRepository.save(new TransactionEntity(
                accountId, DEPOSIT, new BigDecimal("10.00"), EUR, null, null, Instant.now()));

        var response = restTemplate.exchange(
                transactionUrl(ALICE_ACCOUNT_NUMBER, saved.getId()), HttpMethod.GET,
                authorized(ALICE_CODE), TransactionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(saved.getId());
        assertThat(response.getBody().type()).isEqualTo(DEPOSIT);
        assertThat(response.getBody().amountIn()).isEqualByComparingTo("10.00");
        assertThat(response.getBody().accountFrom()).isNull();
        assertThat(response.getBody().accountTo()).isNull();
    }

    @Test
    void returnsTransferWithSourceAndDestinationNumbers() {
        var aliceId = accountRepository.findByNumber(ALICE_ACCOUNT_NUMBER).orElseThrow().getId();
        var bobId = accountRepository.findByNumber(BOB_ACCOUNT_NUMBER).orElseThrow().getId();
        var saved = transactionRepository.save(new TransactionEntity(
                aliceId, bobId, TRANSFER, new BigDecimal("5.00"), EUR, new BigDecimal("5.00"), EUR, Instant.now()));

        var response = restTemplate.exchange(
                transactionUrl(ALICE_ACCOUNT_NUMBER, saved.getId()), HttpMethod.GET,
                authorized(ALICE_CODE), TransactionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().type()).isEqualTo(TRANSFER);
        assertThat(response.getBody().accountFrom()).isEqualTo(ALICE_ACCOUNT_NUMBER);
        assertThat(response.getBody().accountTo()).isEqualTo(BOB_ACCOUNT_NUMBER);
    }

    @Test
    void recipientCanViewTransferTargetingTheirAccount() {
        var aliceId = accountRepository.findByNumber(ALICE_ACCOUNT_NUMBER).orElseThrow().getId();
        var bobId = accountRepository.findByNumber(BOB_ACCOUNT_NUMBER).orElseThrow().getId();
        var saved = transactionRepository.save(new TransactionEntity(
                aliceId, bobId, TRANSFER, new BigDecimal("5.00"), EUR, new BigDecimal("5.00"), EUR, Instant.now()));

        var response = restTemplate.exchange(
                transactionUrl(BOB_ACCOUNT_NUMBER, saved.getId()), HttpMethod.GET,
                authorized(BOB_CODE), TransactionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accountFrom()).isEqualTo(ALICE_ACCOUNT_NUMBER);
        assertThat(response.getBody().accountTo()).isEqualTo(BOB_ACCOUNT_NUMBER);
    }

    @Test
    void accessingAnotherUsersAccountReturnsForbidden() {
        var response = restTemplate.exchange(
                transactionUrl(ALICE_ACCOUNT_NUMBER, UUID.randomUUID()), HttpMethod.GET,
                authorized(BOB_CODE), String.class);

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void unknownTransactionReturnsNotFound() {
        var response = restTemplate.exchange(
                transactionUrl(ALICE_ACCOUNT_NUMBER, UUID.randomUUID()), HttpMethod.GET,
                authorized(ALICE_CODE), String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void transactionBelongingToAnotherAccountReturnsNotFound() {
        var bobId = accountRepository.findByNumber(BOB_ACCOUNT_NUMBER).orElseThrow().getId();
        var bobTransaction = transactionRepository.save(new TransactionEntity(
                bobId, DEPOSIT, new BigDecimal("99.00"), EUR, null, null, Instant.now()));

        var response = restTemplate.exchange(
                transactionUrl(ALICE_ACCOUNT_NUMBER, bobTransaction.getId()), HttpMethod.GET,
                authorized(ALICE_CODE), String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.getForEntity(
                transactionUrl(ALICE_ACCOUNT_NUMBER, UUID.randomUUID()), String.class);

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
