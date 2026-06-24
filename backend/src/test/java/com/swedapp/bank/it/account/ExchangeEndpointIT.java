package com.swedapp.bank.it.account;

import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.TransactionType;
import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.ExchangeResponse;
import com.swedapp.bank.web.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static com.swedapp.bank.domain.Currency.EUR;
import static com.swedapp.bank.domain.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

class ExchangeEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";
    private static final String EXCHANGE_URL = "/api/accounts/exchange";

    @Test
    void exchangeConvertsEurToUsdAndRecordsTransaction() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "EUR",
                        "toCurrency", "USD",
                        "amount", "10.00"), ALICE_CODE),
                ExchangeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accountNumber()).isEqualTo(ALICE_ACCOUNT_NUMBER);
        assertThat(body.fromCurrency()).isEqualTo(EUR);
        assertThat(body.fromBalance()).isEqualByComparingTo("90.00");
        assertThat(body.toCurrency()).isEqualTo(USD);
        assertThat(body.toBalance()).isEqualByComparingTo("111.40");
        assertThat(body.debitedAmount()).isEqualByComparingTo("10.00");
        assertThat(body.creditedAmount()).isEqualByComparingTo("11.40");
        assertThat(body.rate()).isEqualByComparingTo("1.14");

        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, EUR)).isEqualByComparingTo("90.00");
        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, USD)).isEqualByComparingTo("111.40");

        var transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);
        var transaction = transactions.getFirst();
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXCHANGE);
        assertThat(transaction.getAmountOut()).isEqualByComparingTo("10.00");
        assertThat(transaction.getCurrencyOut()).isEqualTo(EUR);
        assertThat(transaction.getAmountIn()).isEqualByComparingTo("11.40");
        assertThat(transaction.getCurrencyIn()).isEqualTo(USD);
    }

    @Test
    void exchangeConvertsUsdToGbpAndLazilyCreatesBalance() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "USD",
                        "toCurrency", "GBP",
                        "amount", "50.00"), ALICE_CODE),
                ExchangeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accountNumber()).isEqualTo(ALICE_ACCOUNT_NUMBER);
        assertThat(body.fromCurrency()).isEqualTo(USD);
        assertThat(body.fromBalance()).isEqualByComparingTo("50.00");
        assertThat(body.toCurrency()).isEqualTo(Currency.GBP);
        assertThat(body.toBalance()).isEqualByComparingTo("37.28");
        assertThat(body.debitedAmount()).isEqualByComparingTo("50.00");
        assertThat(body.creditedAmount()).isEqualByComparingTo("37.28");
        assertThat(body.rate()).isEqualByComparingTo("0.745614");

        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, USD)).isEqualByComparingTo("50.00");
        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, Currency.GBP)).isEqualByComparingTo("37.28");

        var transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);
        var transaction = transactions.getFirst();
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXCHANGE);
        assertThat(transaction.getAmountOut()).isEqualByComparingTo("50.00");
        assertThat(transaction.getCurrencyOut()).isEqualTo(USD);
        assertThat(transaction.getAmountIn()).isEqualByComparingTo("37.28");
        assertThat(transaction.getCurrencyIn()).isEqualTo(Currency.GBP);
    }

    @Test
    void sameCurrencyReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "EUR",
                        "toCurrency", "EUR",
                        "amount", "10.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void insufficientFundsReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "EUR",
                        "toCurrency", "USD",
                        "amount", "1000.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(balanceOf(ALICE_ACCOUNT_NUMBER, EUR)).isEqualByComparingTo("100.00");
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void missingSourceBalanceReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "GBP",
                        "toCurrency", "EUR",
                        "amount", "10.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void nonPositiveAmountReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "EUR",
                        "toCurrency", "USD",
                        "amount", "-10.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void amountAboveMaxReturnsBadRequest() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "EUR",
                        "toCurrency", "USD",
                        "amount", "100000001"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void unknownAccountReturnsNotFound() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", "SE0000000000",
                        "fromCurrency", "EUR",
                        "toCurrency", "USD",
                        "amount", "10.00"), ALICE_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void foreignAccountReturnsForbidden() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                authorized(Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "EUR",
                        "toCurrency", "USD",
                        "amount", "10.00"), BOB_CODE),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.postForEntity(
                EXCHANGE_URL,
                Map.of(
                        "accountNumber", ALICE_ACCOUNT_NUMBER,
                        "fromCurrency", "EUR",
                        "toCurrency", "USD",
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
