package com.n11bootcamp.user_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11bootcamp.user_service.controller.UserController;
import com.n11bootcamp.user_service.request.LoginRequest;
import com.n11bootcamp.user_service.request.SignupRequest;
import com.n11bootcamp.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegisterUser() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setRole(Collections.singleton("user"));

        Mockito.doReturn(ResponseEntity.ok("User registered successfully!"))
                .when(userService).registerUser(any(SignupRequest.class));

        mockMvc.perform(post("/api/user/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void testAuthenticateUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Mockito.doReturn(ResponseEntity.ok("jwt-token"))
                .when(userService).authenticateUser(any(LoginRequest.class));

        mockMvc.perform(post("/api/user/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
