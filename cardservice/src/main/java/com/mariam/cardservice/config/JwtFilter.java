package com.mariam.cardservice.config;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.UserDetails;


import com.mariam.cardservice.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class JwtFilter extends OncePerRequestFilter{

	@Autowired
    private JwtUtil jwtUtil;

	@Value("${gateway.keys.card}")
	private String expectedGatewayKey;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
	        throws ServletException, IOException {
	    
	    String path = request.getRequestURI();
	    String method = request.getMethod();

	    // 1. BYPASS FOR OPTIONS
	    if ("OPTIONS".equalsIgnoreCase(method)) {
	        chain.doFilter(request, response);
	        return; 
	    }

	    // 2. CHECK THE CARD PRIVATE KEY
	    String pvtKey = request.getHeader("MariamPvtKey-Card"); 
	    
	    // CHANGE THIS: If the key is VALID, authenticate as a "System Admin" and bypass JWT check
	    if (pvtKey != null && pvtKey.equals(expectedGatewayKey)) {
	        UserDetails systemUser = new User("SYSTEM", "", Collections.emptyList());
	        UsernamePasswordAuthenticationToken systemAuth = 
	                new UsernamePasswordAuthenticationToken(systemUser, null, systemUser.getAuthorities());
	        SecurityContextHolder.getContext().setAuthentication(systemAuth);
	        
	        chain.doFilter(request, response);
	        return; // EXIT HERE so it doesn't try to look for a JWT!
	    }

	    // 3. IF NO KEY, CHECK JWT (For your frontend)
	    String authHeader = request.getHeader("Authorization");
	    if (authHeader != null && authHeader.startsWith("Bearer ")) {
	        String token = authHeader.substring(7);
	        
	        if (jwtUtil.validateToken(token)) {
	            String email = jwtUtil.extractEmail(token);
	            UserDetails userDetails = new User(email, "", Collections.emptyList());
	            UsernamePasswordAuthenticationToken authToken = 
	                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	            
	            SecurityContextHolder.getContext().setAuthentication(authToken);
	        }
	    }
	    
	    // Final check: if neither key nor JWT worked, Spring Security will 403 naturally
	    chain.doFilter(request, response);
	}
}




