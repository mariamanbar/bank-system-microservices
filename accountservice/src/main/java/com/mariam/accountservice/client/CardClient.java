package com.mariam.accountservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.mariam.accountservice.model.CardRequest;

@FeignClient(name = "card-service", url = "http://localhost:8082") 
public interface CardClient {

    @PostMapping("/api/cards")
    Object issueCard(@RequestBody CardRequest request);
    
//    issueCard(String customerId, String accountId, int pin, String type)
}