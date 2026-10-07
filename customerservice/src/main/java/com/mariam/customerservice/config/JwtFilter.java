package com.mariam.customerservice.config;

import com.mariam.customerservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


@Component
public class JwtFilter extends OncePerRequestFilter{

	@Autowired
    private JwtUtil jwtUtil;

	@Value("${gateway.keys.customer}")
	private String expectedGatewayKey;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
	        throws ServletException, IOException {
		
		
		
		String path = request.getRequestURI();
		String method = request.getMethod();
		// 1. ABSOLUTE BYPASS FOR REGISTRATION AND OPTIONS
	    if (path.contains("/register") || path.contains("/search") || "OPTIONS".equalsIgnoreCase(method)) {
	        filterChain.doFilter(request, response);
	        return; 
	    }

	    // 2. NOW CHECK THE AHMAD KEY FOR EVERYTHING ELSE
	    String pvtKey = request.getHeader("MariamPvtKey-Cust");
	    if (pvtKey == null || !pvtKey.equals(expectedGatewayKey)) {
	        System.out.println("CUSTOMER SERVICE FORBIDDEN: Missing key for path: " + path);
	        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	        return;
	    }
	    
	    

	    // B. Standard JWT Logic
	    String authHeader = request.getHeader("Authorization");
	    if (authHeader != null && authHeader.startsWith("Bearer ")) {
	        String token = authHeader.substring(7);
	        if (jwtUtil.validateToken(token)) {
	            String email = jwtUtil.extractEmail(token);
	            String role = jwtUtil.extractRole(token);
	            
	            // Set context with role
	            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
	                email, null, List.of(new SimpleGrantedAuthority(role)));
	            SecurityContextHolder.getContext().setAuthentication(authToken);
	        }
	    }
	    filterChain.doFilter(request, response);
	}
}

