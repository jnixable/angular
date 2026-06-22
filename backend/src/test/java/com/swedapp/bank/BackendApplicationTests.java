package com.swedapp.bank;

import com.swedapp.bank.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.security.jwt.secret=test-secret-key-with-at-least-256-bits-for-hmac-sha256")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
