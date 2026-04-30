package com.n11bootcamp.api_gateway.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    // Token -> RefreshToken mapping
    private final Map<String, RefreshToken> refreshTokenMap = new HashMap<>();

    // Refresh token validity: 100 minutes
    private static final long REFRESH_TOKEN_VALIDITY_MS = 200 * 60 * 1000;

    public String createRefreshToken(String username) {
        // Delete existing tokens for this user if any (optional, for simple rotation)
        refreshTokenMap.values().removeIf(rt -> rt.username.equals(username));

        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.token = token;
        refreshToken.username = username;
        refreshToken.expiryDate = Instant.now().plusMillis(REFRESH_TOKEN_VALIDITY_MS);

        refreshTokenMap.put(token, refreshToken);
        return token;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenMap.get(token);
    }

    public boolean verifyExpiration(RefreshToken token) {
        if (token.expiryDate.isBefore(Instant.now())) {
            refreshTokenMap.remove(token.token);
            return false;
        }
        return true;
    }

    public void deleteByToken(String token) {
        refreshTokenMap.remove(token);
    }

    public static class RefreshToken {
        private String token;
        private String username;
        private Instant expiryDate;

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }

        public Instant getExpiryDate() {
            return expiryDate;
        }
    }
}
