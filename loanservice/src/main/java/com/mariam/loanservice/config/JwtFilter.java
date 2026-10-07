package com.mariam.loanservice.config;

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

import com.mariam.loanservice.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter{

	@Autowired
    private JwtUtil jwtUtil;

	@Value("${gateway.keys.loan}")
	private String expectedGatewayKey;
	
	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
		
		String gatewayKey = request.getHeader("MariamPvtKey-Loan");
		// Check if it matches exactly what you put in the Gateway
		if ("OPTIONS".equals(request.getMethod())) {
			chain.doFilter(request, response);
		    return;
		}

		if (gatewayKey == null || !gatewayKey.equals(expectedGatewayKey)) {
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

