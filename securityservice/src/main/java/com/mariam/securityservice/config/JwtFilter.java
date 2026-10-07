package com.mariam.securityservice.config;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

	@Value("${gateway.keys.auth}")
	private String expectedGatewayKey;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. ALWAYS LET OPTIONS PASS FIRST (Preflight handshake)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. SKIP EVERYTHING FOR LOGIN AND REGISTER
        if (path.contains("/login") || path.contains("/register")) {
            chain.doFilter(request, response);
            return;
        }
        
        // 3. GATEWAY SECURITY CHECK (The "Ahmad Key")
        // If we reach here, it is an internal request that MUST have the key
        String pvtKey = request.getHeader("MariamPvtKey-Auth");
        if (pvtKey == null || !pvtKey.equals(expectedGatewayKey)) {
            System.out.println("FORBIDDEN: Missing or invalid MariamPvtKey-Auth for path: " + path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return; // Block direct access
        }

        // 4. JWT TOKEN VALIDATION
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                // EXTRACT THE ROLE (Important for the dynamic dashboard!)
                String role = jwtUtil.extractRole(token); 
                
                if (email != null) {
                    // Use the role from the token to create authorities
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role != null ? role : "CUSTOMER"));

                    UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        
        chain.doFilter(request, response);
    }
}