package com.mariam.cardservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
public class SecurityConfig {

	@Autowired
    private JwtFilter jwtFilter;
	

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	    http
	        .csrf(csrf -> csrf.disable())
	        // DISABLE local CORS here because the Gateway handles it
	        .cors(cors -> cors.disable()) 
	        .authorizeHttpRequests(auth -> auth
	            // 1. ALWAYS permit OPTIONS requests
	            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
	            .requestMatchers("/h2-console/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
	            .anyRequest().authenticated()
	        )
	        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

	    return http.build();
	}
	
}

