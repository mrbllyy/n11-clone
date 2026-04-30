package com.n11bootcamp.user_service.controller;

import com.n11bootcamp.user_service.request.LoginRequest;
import com.n11bootcamp.user_service.request.SignupRequest;
import com.n11bootcamp.user_service.request.UpdateUserRequest;
import com.n11bootcamp.user_service.service.UserService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping("/internal/{username}")
    public ResponseEntity<?> findByUsername(@PathVariable String username) {
        return userService.findByUsername(username);
    }
    @PostMapping("/signin")

    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return userService.authenticateUser(loginRequest);
    }

    @PostMapping("/signup")

    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return userService.registerUser(signUpRequest);
    }

    @DeleteMapping("/delete/{userId}")

    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        return userService.deleteUser(userId);
    }

    @PutMapping("/update/{userId}")

    public ResponseEntity<?> updateUser(@PathVariable Long userId,
                                        @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        return userService.updateUser(userId, updateUserRequest);
    }

}