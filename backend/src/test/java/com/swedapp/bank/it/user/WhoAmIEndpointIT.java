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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.swedapp.bank.db.entity.UserEntity;
import com.swedapp.bank.db.repository.UserRepository;
import com.swedapp.bank.web.dto.LoginResponse;
import com.swedapp.bank.web.dto.WhoAmIResponse;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = "app.security.jwt.secret=test-secret-key-with-at-least-256-bits-for-hmac-sha256")
class WhoAmIEndpointIT {

    private static final String PCODE = "19900101001";
    private static final String PASSWORD = "secret-pass-123";
    private static final String FIRSTNAME = "Test";
    private static final String LASTNAME = "User";
    private static final String EMAIL = "test.user@swedapp.com";
    private static final String LOGIN_URL = "/api/user/login";
    private static final String WHOAMI_URL = "/api/user/whoami";

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
                FIRSTNAME,
                LASTNAME,
                EMAIL,
                passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void authenticatedRequestReturnsCurrentUser() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(login());

        var response = restTemplate.exchange(
                WHOAMI_URL, HttpMethod.GET, new HttpEntity<>(headers), WhoAmIResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().pCode()).isEqualTo(PCODE);
        assertThat(response.getBody().firstName()).isEqualTo(FIRSTNAME);
        assertThat(response.getBody().lastName()).isEqualTo(LASTNAME);
        assertThat(response.getBody().email()).isEqualTo(EMAIL);
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        var response = restTemplate.getForEntity(WHOAMI_URL, String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    private String login() {
        var response = restTemplate.postForEntity(
                LOGIN_URL,
                Map.of("pcode", PCODE, "password", PASSWORD),
                LoginResponse.class);
        return response.getBody().token();
    }
}
