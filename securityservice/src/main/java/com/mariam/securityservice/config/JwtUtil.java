package com.mariam.securityservice.config;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

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
	public String generateToken(String email, String role) {
	    Map<String, Object> claims = new HashMap<>();
	    claims.put("role", role); // Add the role to the token!

	    return Jwts.builder()
	            .setClaims(claims)
	            .setSubject(email)
	            .setIssuedAt(new Date())
	            .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
	            .signWith(SECRET_KEY)
	            .compact();
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
    
    public String extractRole(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY).build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
    }
}
