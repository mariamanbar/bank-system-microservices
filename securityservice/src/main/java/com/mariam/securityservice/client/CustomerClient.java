package com.mariam.securityservice.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "customer-service", url = "http://localhost:8080/api/customers")
public interface CustomerClient {
    
    @PostMapping("/register")
    ResponseEntity<?> saveCustomer(@RequestBody Map<String, Object> customerData, 
            @RequestHeader("MariamPvtKey-Cust") String pvtKey);
    

    @GetMapping("/search")
    ResponseEntity<Map<String, Object>> getCustomerByEmail(@RequestParam String email, 
    		@RequestHeader("MariamPvtKey-Cust") String pvtKey);
}
