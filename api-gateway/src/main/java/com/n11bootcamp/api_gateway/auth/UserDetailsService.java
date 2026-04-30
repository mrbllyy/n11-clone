package com.n11bootcamp.api_gateway.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import org.springframework.web.client.RestTemplate;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            // USER-SERVICE üzerinden kullanıcıyı sorgula
            Map user = restTemplate.getForObject("http://USER-SERVICE/api/user/internal/" + username, Map.class);

            if (user != null && user.containsKey("username") && user.containsKey("password")) {
                return new User(
                        (String) user.get("username"),
                        (String) user.get("password"),
                        new ArrayList<>()
                );
            }
        } catch (Exception e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
