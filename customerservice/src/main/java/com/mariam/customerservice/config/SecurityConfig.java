package com.mariam.customerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class SecurityConfig {

	@Autowired
    private JwtFilter jwtFilter;
	
	@Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF (Cross-Site Request Forgery, it blocks POST)
            // 2. ALLOW FRAMES (This fixes the "Refused to connect" error)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            
            .authorizeHttpRequests(auth -> auth
                //Allow Login and Register endpoints 
                .requestMatchers("/api/customers/login", "/api/customers/register").permitAll()
                //Allow Swagger
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/h2-console/**"
                    ).permitAll()
                // Lock everything else
                .anyRequest().permitAll()
            )
            // Add custom JWT Filter before username..
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
	
}
