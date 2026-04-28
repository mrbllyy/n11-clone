package com.n11bootcamp.api_gateway.controller;

import com.n11bootcamp.api_gateway.auth.RefreshTokenService;
import com.n11bootcamp.api_gateway.auth.TokenManager;
import com.n11bootcamp.api_gateway.request.AuthResponse;
import com.n11bootcamp.api_gateway.request.LoginRequest;
import com.n11bootcamp.api_gateway.request.RefreshTokenRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AuthController {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            String accessToken = tokenManager.generateToken(loginRequest.getUsername());
            String refreshToken = refreshTokenService.createRefreshToken(loginRequest.getUsername());

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        String token = refreshTokenRequest.getRefreshToken();
        RefreshTokenService.RefreshToken refreshToken = refreshTokenService.findByToken(token);

        if (refreshToken != null && refreshTokenService.verifyExpiration(refreshToken)) {
            String username = refreshToken.getUsername();

            // Generate new tokens (Rotation)
            String newAccessToken = tokenManager.generateToken(username);
            String newRefreshToken = refreshTokenService.createRefreshToken(username);

            // Invalidate the old one
            refreshTokenService.deleteByToken(token);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));
        } else {
            return ResponseEntity.status(403).body(null);
        }
    }

}
