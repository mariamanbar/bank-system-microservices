package com.mariam.apigateway;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

@Component
@Slf4j
public class GWRoutingFilter implements WebFilter {

    private static final Map<String, String> service = new HashMap<>();

	@Value("${gateway.keys.account}")
	private String accountKey;

	@Value("${gateway.keys.customer}")
	private String customerKey;

	@Value("${gateway.keys.card}")
	private String cardKey;

	@Value("${gateway.keys.loan}")
	private String loanKey;

	@Value("${gateway.keys.logger}")
	private String loggerKey;

	@Value("${gateway.keys.auth}")
	private String authKey;
    static {
        service.put("/account", "http://localhost:8081/api/accounts");
        service.put("/customer", "http://localhost:8080/api/customers");
        service.put("/card", "http://localhost:8082/api/cards");
        service.put("/loan", "http://localhost:8083/api/loans");
        service.put("/logger", "http://localhost:8086/api/logs");
        service.put("/auth", "http://localhost:8087/api/auth");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    	ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // --- MUST BE AT THE VERY TOP ---
        // Handle CORS Preflight (The browser's "handshake")
        if (request.getMethod() == HttpMethod.OPTIONS) {
            response.getHeaders().add("Access-Control-Allow-Origin", "http://127.0.0.1:5500");
            response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            response.getHeaders().add("Access-Control-Allow-Headers", "*");
            response.getHeaders().add("Access-Control-Max-Age", "3600");
            response.setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }
        
        String path = exchange.getRequest().getURI().getPath();
        log.info("[Gateway] Incoming Request: {}", path);

        for (Map.Entry<String, String> entry : service.entrySet()) {
            String prefix = entry.getKey(); 
            
            if (path.startsWith(prefix)) { 
                String targetBaseUrl = entry.getValue();
                
                // Dynamic Pathing: extract everything after the prefix (e.g., /debit, /status, /pay)
                String subPath = path.substring(prefix.length()); 
                String query = exchange.getRequest().getURI().getQuery();
                String fullUri = targetBaseUrl + subPath + (query != null ? "?" + query : "");

                log.info("[Gateway] Routing matched prefix '{}' to destination: {}", prefix, fullUri);

                WebClient webClient = WebClient.create(); 

                return webClient
                    .method(exchange.getRequest().getMethod())
                    .uri(fullUri)
                    .headers(headers -> {
                        // Forward original headers
                        headers.addAll(exchange.getRequest().getHeaders());
                        
                        // --- AHMAD'S SECURITY KEYS ---
                        if (prefix.equals("/account")) {
                            headers.add("MariamPvtKey-Account", accountKey);
                        } else if (prefix.equals("/customer")) {
                            headers.add("MariamPvtKey-Cust", customerKey);
                        } else if (prefix.equals("/card")) {
                            headers.add("MariamPvtKey-Card", cardKey);
                        } else if (prefix.equals("/loan")) {
                            headers.add("MariamPvtKey-Loan", loanKey);
                        } else if (prefix.equals("/logger")) {
                            headers.add("MariamPvtKey-Log", loggerKey);
                        } else if (prefix.equals("/auth")) {
                            headers.add("MariamPvtKey-Auth", authKey);
                        }
                    })
                    .contentType(exchange.getRequest().getHeaders().getContentType() != null 
                        ? exchange.getRequest().getHeaders().getContentType() 
                        : MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromDataBuffers(exchange.getRequest().getBody()))
                    .exchangeToMono(clientResponse -> {
                        log.info("[Gateway] Service Response: {} for path {}", clientResponse.statusCode(), path);
                        
                        // 1. Set the status code from the microservice
                        exchange.getResponse().setStatusCode(clientResponse.statusCode());
                        
                        // 2. Copy original headers from the microservice
                        exchange.getResponse().getHeaders().addAll(clientResponse.headers().asHttpHeaders());
                        
                        // 3. --- THE CORS FIX: FORCE HEADERS ON THE WAY OUT ---
                        // This ensures the browser always sees these headers
                        exchange.getResponse().getHeaders().set("Access-Control-Allow-Origin", "http://127.0.0.1:5500");
                        exchange.getResponse().getHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                        exchange.getResponse().getHeaders().set("Access-Control-Allow-Headers", "*");
                        exchange.getResponse().getHeaders().set("Access-Control-Allow-Credentials", "true");
                        // ----------------------------------------------------

                        return exchange.getResponse().writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                    })
                    .doOnError(e -> log.error("[Gateway] CRITICAL ERROR forwarding to {}: {}", prefix, e.getMessage()));
            }
        }

        log.warn("[Gateway] No routing match for path: {}", path);
        return chain.filter(exchange);
    }
}