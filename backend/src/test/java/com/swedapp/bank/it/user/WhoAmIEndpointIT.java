package com.swedapp.bank.it.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.LoginResponse;

class WhoAmIEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";
    private static final String WHOAMI_URL = "/api/user/whoami";

    @Test
    void authenticatedRequestReturnsCurrentUser() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(login());

        var response = restTemplate.exchange(
                WHOAMI_URL, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        var body = response.getBody();
        assertThat(body.get("code").asText()).isEqualTo(ALICE_CODE);
        assertThat(body.get("userDetails").get("firstname").asText()).isEqualTo(ALICE_FIRSTNAME);
        assertThat(body.get("userDetails").get("lastname").asText()).isEqualTo(ALICE_LASTNAME);
        assertThat(body.get("email").asText()).isEqualTo(ALICE_EMAIL);
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.getForEntity(WHOAMI_URL, String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    private String login() {
        var response = restTemplate.postForEntity(
                LOGIN_URL,
                Map.of("code", ALICE_CODE, "password", PASSWORD),
                LoginResponse.class);
        return response.getBody().token();
    }
}
