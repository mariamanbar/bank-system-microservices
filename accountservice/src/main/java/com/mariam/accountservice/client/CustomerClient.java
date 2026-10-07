package com.mariam.accountservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

// name: The service name in Eureka (or just a label)
// url: The actual address of your Customer Microservice
@FeignClient(name = "customer-service", url = "http://localhost:8080")
public interface CustomerClient {

    /**
     * Calls the GET /api/customers endpoint we built earlier.
     * It will pass the ?id=... parameter to check if the user exists.
     */
    @GetMapping(value = "/api/customers", produces = "application/json")
    Map<String, Object> getCustomerById(@RequestParam("id") String id, @RequestHeader("MariamPvtKey-Cust") String pvtKey);
}