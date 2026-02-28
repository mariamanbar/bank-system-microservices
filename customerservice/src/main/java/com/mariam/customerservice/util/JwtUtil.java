package com.mariam.customerservice.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
    private String secretString;
	
	private Key SECRET_KEY;
	
	@PostConstruct
    public void init() {
        this.SECRET_KEY = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }
	
	// Generate Token
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(SECRET_KEY)
                .compact(); // To a String
    }
    
    //  Get Email from Token
    public String extractEmail(String token) {
        return Jwts.parserBuilder().
        		setSigningKey(SECRET_KEY).build()
                .parseClaimsJws(token)    // Read the token
                .getBody().getSubject();
    }
    
    // Validate Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token); // Checks the Token
            return true;
        } catch (Exception e) {
            return false;  // Access Denied
        }
    }
}
