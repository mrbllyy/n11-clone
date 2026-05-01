package com.n11bootcamp.api_gateway.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, RefreshToken> fallbackStore = new ConcurrentHashMap<>();

    @Value("${refresh-token.validity-ms:1209600000}")
    private long refreshTokenValidityMs;

    @Value("${refresh-token.redis-prefix:n11:refresh-token}")
    private String redisPrefix;

    public RefreshTokenService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public String createRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken(
                token,
                username,
                Instant.now().plusMillis(refreshTokenValidityMs)
        );

        if (storeInRedis(refreshToken)) {
            return token;
        }

        fallbackStore.values().removeIf(existing -> existing.username.equals(username));
        fallbackStore.put(token, refreshToken);
        return token;
    }

    public RefreshToken findByToken(String token) {
        RefreshToken redisToken = findInRedis(token);
        if (redisToken != null) {
            return redisToken;
        }
        return fallbackStore.get(token);
    }

    public boolean verifyExpiration(RefreshToken token) {
        if (token == null) {
            return false;
        }
        if (token.expiryDate.isBefore(Instant.now())) {
            deleteByToken(token.token);
            return false;
        }
        return true;
    }

    public void deleteByToken(String token) {
        deleteFromRedis(token);
        fallbackStore.remove(token);
    }

    private boolean storeInRedis(RefreshToken refreshToken) {
        if (redisTemplate == null) {
            return false;
        }

        try {
            String previousToken = redisTemplate.opsForValue().get(userKey(refreshToken.username));
            if (previousToken != null) {
                redisTemplate.delete(tokenKey(previousToken));
            }

            Duration ttl = Duration.ofMillis(refreshTokenValidityMs);
            redisTemplate.opsForValue().set(tokenKey(refreshToken.token), refreshToken.username, ttl);
            redisTemplate.opsForValue().set(userKey(refreshToken.username), refreshToken.token, ttl);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private RefreshToken findInRedis(String token) {
        if (redisTemplate == null) {
            return null;
        }

        try {
            String username = redisTemplate.opsForValue().get(tokenKey(token));
            if (username == null) {
                return null;
            }

            Long ttlSeconds = redisTemplate.getExpire(tokenKey(token));
            if (ttlSeconds == null || ttlSeconds <= 0) {
                return null;
            }

            return new RefreshToken(token, username, Instant.now().plusSeconds(ttlSeconds));
        } catch (Exception ex) {
            return null;
        }
    }

    private void deleteFromRedis(String token) {
        if (redisTemplate == null) {
            return;
        }

        try {
            String username = redisTemplate.opsForValue().get(tokenKey(token));
            redisTemplate.delete(tokenKey(token));
            if (username != null) {
                redisTemplate.delete(userKey(username));
            }
        } catch (Exception ignored) {
        }
    }

    private String tokenKey(String token) {
        return redisPrefix + ":token:" + token;
    }

    private String userKey(String username) {
        return redisPrefix + ":user:" + username;
    }

    public static class RefreshToken {
        private final String token;
        private final String username;
        private final Instant expiryDate;

        public RefreshToken(String token, String username, Instant expiryDate) {
            this.token = token;
            this.username = username;
            this.expiryDate = expiryDate;
        }

        public String getToken() { return token; }
        public String getUsername() { return username; }
        public Instant getExpiryDate() { return expiryDate; }
    }
}
