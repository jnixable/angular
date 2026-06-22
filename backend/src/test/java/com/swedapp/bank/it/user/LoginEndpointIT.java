package com.swedapp.bank.it.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.swedapp.bank.it.BaseIT;
import com.swedapp.bank.web.dto.LoginResponse;

class LoginEndpointIT extends BaseIT {

    private static final String LOGIN_URL = "/api/user/login";

    @Test
    void validCredentialsReturnToken() {
        var response = restTemplate.postForEntity(
                LOGIN_URL,
                Map.of("code", ALICE_CODE, "password", PASSWORD),
                LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().expiresInSeconds()).isPositive();
    }

    @Test
    void wrongPasswordReturnsUnauthorized() {
        var response = restTemplate.postForEntity(
                LOGIN_URL,
                Map.of("code", ALICE_CODE, "password", "wrong-password"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    void unknownUserReturnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                LOGIN_URL,
                Map.of("code", "00000000000", "password", PASSWORD),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }
}
