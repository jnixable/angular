package com.swedapp.bank.it.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;

import com.swedapp.bank.config.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.swedapp.bank.db.entity.UserEntity;
import com.swedapp.bank.db.repository.UserRepository;
import com.swedapp.bank.web.dto.LoginResponse;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "app.security.jwt.secret=test-secret-key-with-at-least-256-bits-for-hmac-sha256")
class LoginEndpointIT {

    private static final String PCODE = "19900101001";
    private static final String PASSWORD = "secret-pass-123";
    private static final String LOGIN_URL = "/api/user/login";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedTestUser() {
        userRepository.deleteAll();
        userRepository.save(new UserEntity(
                PCODE,
                "Test",
                "User",
                "test.user@swedapp.com",
                passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void validCredentialsReturnToken() {
        var response = restTemplate.postForEntity(
                LOGIN_URL,
                Map.of("pcode", PCODE, "password", PASSWORD),
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
                Map.of("pcode", PCODE, "password", "wrong-password"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    void unknownUserReturnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                LOGIN_URL,
                Map.of("pcode", "00000000000", "password", PASSWORD),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }
}
