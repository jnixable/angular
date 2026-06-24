package com.swedapp.bank.it.account;

import com.swedapp.bank.db.entity.AccountBalanceEntity;
import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.TransactionType;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.service.txchecker.TxCheckerClient;
import com.swedapp.bank.service.txchecker.TxCheckerResponse;
import com.swedapp.bank.web.dto.LoginResponse;
import com.swedapp.bank.web.dto.TransactionResponse;
import com.swedapp.bank.web.dto.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;

class TransferEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";
    private static final String TRANSFER_URL = "/api/accounts/transfer";
    private static final String ALICE_SECOND_ACCOUNT_NUMBER = "LVTEST0000000003";

    @MockitoBean
    private TxCheckerClient txCheckerClient;

    @BeforeEach
    void stubTxChecker() {
        when(txCheckerClient.check(any(), any(), any()))
                .thenReturn(new TxCheckerResponse(TxCheckerResponse.Status.APPROVED, null));
    }

    @Test
    void transferBetweenUsersInEurMovesFunds() {
        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", BOB_ACCOUNT_NUMBER,
                        "currency", "EUR",
                        "amount", "30.00"), ALICE_CODE),
                TransferResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sourceAccountNumber()).isEqualTo(ALICE_ACCOUNT_NUMBER);
        assertThat(response.getBody().destinationAccountNumber()).isEqualTo(BOB_ACCOUNT_NUMBER);
        assertThat(response.getBody().currency()).isEqualTo(Currency.EUR);
        assertThat(response.getBody().amount()).isEqualByComparingTo("30.00");
        assertThat(response.getBody().sourceBalance()).isEqualByComparingTo("70.00");

        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, Currency.EUR)).isEqualByComparingTo("70.00");
        assertThat(balanceOf(BOB_ACCOUNT_NUMBER, Currency.EUR)).isEqualByComparingTo("130.00");
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void transferBetweenUsersInNonEurReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", BOB_ACCOUNT_NUMBER,
                        "currency", "USD",
                        "amount", "30.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, Currency.USD)).isEqualByComparingTo("100.00");
    }

    @Test
    void transferBetweenOwnAccountsInNonEurMovesFunds() {
        seedSecondAliceAccount();

        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", ALICE_SECOND_ACCOUNT_NUMBER,
                        "currency", "USD",
                        "amount", "40.00"), ALICE_CODE),
                TransferResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sourceBalance()).isEqualByComparingTo("60.00");

        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, Currency.USD)).isEqualByComparingTo("60.00");
        assertThat(balanceOf(ALICE_SECOND_ACCOUNT_NUMBER, Currency.USD)).isEqualByComparingTo("40.00");
    }

    @Test
    void insufficientFundsReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", BOB_ACCOUNT_NUMBER,
                        "currency", "EUR",
                        "amount", "1000.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, Currency.EUR)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(BOB_ACCOUNT_NUMBER, Currency.EUR)).isEqualByComparingTo("100.00");
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void unknownDestinationReturnsNotFound() {
        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", "LVTEST0000000999",
                        "currency", "EUR",
                        "amount", "30.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, Currency.EUR)).isEqualByComparingTo("100.00");
    }

    @Test
    void transferFromNotOwnedAccountReturnsForbidden() {
        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", BOB_ACCOUNT_NUMBER,
                        "destinationAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "currency", "EUR",
                        "amount", "30.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
        assertThat(balanceOf(BOB_ACCOUNT_NUMBER, Currency.EUR)).isEqualByComparingTo("100.00");
    }

    @Test
    void sameSourceAndDestinationReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "currency", "EUR",
                        "amount", "30.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void rejectedByTxCheckerReturnsUnprocessableEntityAndKeepsBalances() {
        when(txCheckerClient.check(any(), any(), any()))
                .thenReturn(new TxCheckerResponse(TxCheckerResponse.Status.REJECTED, "Magic is not allowed here"));

        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", BOB_ACCOUNT_NUMBER,
                        "currency", "EUR",
                        "amount", "30.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, Currency.EUR)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(BOB_ACCOUNT_NUMBER, Currency.EUR)).isEqualByComparingTo("100.00");
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void transferAppearsInBothAccountsHistories() {
        restTemplate.postForEntity(
                TRANSFER_URL,
                authorized(Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", BOB_ACCOUNT_NUMBER,
                        "currency", "EUR",
                        "amount", "30.00"), ALICE_CODE),
                TransferResponse.class);

        var aliceHistory = restTemplate.exchange(
                "/api/accounts/" + ALICE_ACCOUNT_NUMBER + "/transactions", HttpMethod.GET,
                authorized(ALICE_CODE), pageType());
        var bobHistory = restTemplate.exchange(
                "/api/accounts/" + BOB_ACCOUNT_NUMBER + "/transactions", HttpMethod.GET,
                authorized(BOB_CODE), pageType());

        assertThat(aliceHistory.getStatusCode()).isEqualTo(OK);
        assertThat(aliceHistory.getBody().content())
                .extracting(TransactionResponse::type)
                .containsExactly(TransactionType.TRANSFER);
        assertThat(bobHistory.getStatusCode()).isEqualTo(OK);
        assertThat(bobHistory.getBody().content())
                .extracting(TransactionResponse::type)
                .containsExactly(TransactionType.TRANSFER);
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.postForEntity(
                TRANSFER_URL,
                Map.of(
                        "sourceAccountNumber", ALICE_ACCOUNT_NUMBER,
                        "destinationAccountNumber", BOB_ACCOUNT_NUMBER,
                        "currency", "EUR",
                        "amount", "30.00"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    private void seedSecondAliceAccount() {
        var account = accountRepository.save(new AccountEntity("Alice Savings", ALICE_SECOND_ACCOUNT_NUMBER, ALICE_CODE));
        accountBalanceRepository.save(new AccountBalanceEntity(account.getId(), Currency.EUR, new BigDecimal("0.00")));
    }

    private ParameterizedTypeReference<PagedTransactions> pageType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private HttpEntity<Map<String, Object>> authorized(Map<String, Object> body, String code) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(login(code));
        return new HttpEntity<>(body, headers);
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
