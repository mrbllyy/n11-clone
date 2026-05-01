package com.n11bootcamp.user_service.service;

import com.n11bootcamp.user_service.entity.ShoppingCart;
import com.n11bootcamp.user_service.entity.User;
import com.n11bootcamp.user_service.repository.UserRepository;
import com.n11bootcamp.user_service.request.LoginRequest;
import com.n11bootcamp.user_service.request.SignupRequest;
import com.n11bootcamp.user_service.request.UpdateUserRequest;
import com.n11bootcamp.user_service.response.JwtResponse;
import com.n11bootcamp.user_service.response.MessageResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<?> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("User credentials are not valid"));
        }

        return ResponseEntity.ok(new JwtResponse(
                null,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()));
    }

    public ResponseEntity<?> registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is already in use!"));
        }

        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword()),
                resolveRole(signUpRequest));

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    public ResponseEntity<?> deleteUser(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found!"));

            deleteShoppingCartIfExists(user);
            userRepository.delete(user);

            return ResponseEntity.ok(new MessageResponse("User account deleted successfully!"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Internal Server Error"));
        }
    }

    public ResponseEntity<?> updateUser(Long userId, UpdateUserRequest updateUserRequest) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found!"));

            if (updateUserRequest.getPassword() != null && !updateUserRequest.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(updateUserRequest.getPassword()));
            }

            if (updateUserRequest.getEmail() != null && !updateUserRequest.getEmail().isBlank()) {
                boolean emailBelongsToSameUser = updateUserRequest.getEmail().equalsIgnoreCase(user.getEmail());
                if (!emailBelongsToSameUser && userRepository.existsByEmail(updateUserRequest.getEmail())) {
                    return ResponseEntity.badRequest().body(new MessageResponse("Email is already in use!"));
                }
                user.setEmail(updateUserRequest.getEmail());
            }

            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("User account updated successfully!"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Internal Server Error"));
        }
    }

    private String resolveRole(SignupRequest signUpRequest) {
        if (signUpRequest.getRole() == null || signUpRequest.getRole().isEmpty()) {
            return "Customer";
        }
        return signUpRequest.getRole().iterator().next();
    }

    private void deleteShoppingCartIfExists(User user) {
        try {
            ShoppingCart shoppingCart = restTemplate.getForObject(
                    "http://SHOPPING-CART-SERVICE/api/shopping-cart/by-name/" + user.getUsername(),
                    ShoppingCart.class);

            if (shoppingCart != null && shoppingCart.getId() != 0L) {
                restTemplate.delete("http://SHOPPING-CART-SERVICE/api/shopping-cart/" + shoppingCart.getId());
            }
        } catch (Exception ignored) {
            // Cart absence should not block user deletion.
        }
    }
}
