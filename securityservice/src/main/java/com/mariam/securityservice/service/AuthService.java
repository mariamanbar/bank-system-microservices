package com.mariam.securityservice.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.mariam.securityservice.client.CustomerClient;
import com.mariam.securityservice.config.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerClient customerClient; // The Feign Client
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

	@Value("${gateway.keys.customer}")
	private String customerServiceKey;

    public String login(String email, String password) {
        // 1. Fetch user data from Customer Service via Feign
    	ResponseEntity<Map<String, Object>> response = customerClient.getCustomerByEmail(email, customerServiceKey);
        Map<String, Object> customerData = response.getBody();

        if (customerData != null) {
            String encodedPassword = (String) customerData.get("password");
            
            // 2. Check if the password is correct
            if (passwordEncoder.matches(password, encodedPassword)) {
                
                // --- THE DYNAMIC ROLE LOGIC ---
                // If the email ends with @arabbank.com.jo, they are an ADMIN
                // Otherwise, they are a regular CUSTOMER
                String role = email.toLowerCase().endsWith("@bank.com.jo") ? "ADMIN" : "CUSTOMER";
                
                // 3. Generate token with the determined role
                
                return jwtUtil.generateToken(email, role);
            }
        }
        throw new RuntimeException("Invalid Credentials");
    }

    public ResponseEntity<?> register(Map<String, Object> data) {
        // 3. Hash password BEFORE sending it to Customer DB
        String rawPassword = (String) data.get("password");
        data.put("password", passwordEncoder.encode(rawPassword));
        
        // 4. Send to Customer Service to save
        return customerClient.saveCustomer(data, customerServiceKey);
    }
}