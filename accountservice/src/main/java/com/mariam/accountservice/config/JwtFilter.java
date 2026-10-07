package com.mariam.accountservice.config;


import jakarta.servlet.FilterChain;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mariam.accountservice.util.JwtUtil;

import java.io.IOException;
import java.util.Collections;


@Component
public class JwtFilter extends OncePerRequestFilter{

	@Autowired
    private JwtUtil jwtUtil;

	@Value("${gateway.keys.account}")
	private String expectedGatewayKey;
	
	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
		
		String path = request.getRequestURI();
	    String method = request.getMethod();

	    // 1. ABSOLUTE BYPASS FOR OPTIONS (Crucial for frontend POST/PUT/DELETE requests)
	    if ("OPTIONS".equalsIgnoreCase(method)) {
	    	chain.doFilter(request, response);
	        return; 
	    }

	    // 2. GATEWAY KEY CHECK 
	    // (Check your GWRoutingFilter in Gateway to see exactly what header/key you used for Account!)
	    // Assuming it's something like this:
	    String pvtKey = request.getHeader("MariamPvtKey-Account"); 
	    if (pvtKey == null || !pvtKey.equals(expectedGatewayKey)) {
	        System.out.println("ACCOUNT SERVICE FORBIDDEN: Missing key for path: " + path);
	        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	        return;
	    }
		
		

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Remove "Bearer " word
            
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                
                // Create a generic user (We just need the email to be authenticated)
                //                                  who | pass | roles 
                UserDetails userDetails = new User(email, "", Collections.emptyList());

                UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }
}

