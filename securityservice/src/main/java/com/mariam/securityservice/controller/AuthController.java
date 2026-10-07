package com.mariam.securityservice.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariam.securityservice.client.CustomerClient;
import com.mariam.securityservice.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CustomerClient customerClient;

	@Value("${gateway.keys.customer}")
	private String customerServiceKey;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> data) {
        log.info("Security Service: Registering user {}", data.get("email"));
        return authService.register(data);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> creds) {
        String email = creds.get("email");
        String password = creds.get("password");
        
        // 1. Pass the "Ahmad Key" here to avoid the 403
        var customerResponse = customerClient.getCustomerByEmail(email, customerServiceKey);
        Map<String, Object> customerData = customerResponse.getBody();
        
        // 2. Perform the actual login (Ensure you updated authService.login too!)
        String token = authService.login(email, password);

        // 3. Determine the role (Domain-based logic if DB column is missing)
        String role;
        if (customerData != null && customerData.get("role") != null) {
            role = (String) customerData.get("role");
        } else {
            // Fallback to your domain-based logic we discussed
            role = email.toLowerCase().endsWith("@bank.jo.com") ? "ADMIN" : "CUSTOMER";
        }

        // 4. Return both to the frontend
        return ResponseEntity.ok(Map.of(
            "token", token,
            "role", role,
            "email", email // Useful for filtering data tables later!
        ));
    }
}