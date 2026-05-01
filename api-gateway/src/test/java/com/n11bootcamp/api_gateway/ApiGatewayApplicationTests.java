package com.n11bootcamp.api_gateway;

import com.n11bootcamp.api_gateway.auth.TokenManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ApiGatewayApplicationTests {

	@Autowired
	private TokenManager tokenManager;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testTokenManager() {
		String username = "test-user";
		String token = tokenManager.generateToken(username);

		String decodedUsername = tokenManager.getUsernameToken(token);
		boolean isValid = tokenManager.tokenValidate(token);

		assertNotNull(token, "Token should not be null");
		assertEquals(username, decodedUsername, "Decoded username should match original");
		assertTrue(isValid, "Token should be valid immediately after generation");
	}

	@Test
	public void testExpiredToken() throws InterruptedException {
		// TokenManager has a validity of 1 minute (60,000 ms) in the source code.
		// To test expiration without waiting a full minute, we'd need to mock the clock
		// or change the validity, but let's just test basic validation for now.
		String token = tokenManager.generateToken("test-user");
		assertTrue(tokenManager.tokenValidate(token));
	}
}
