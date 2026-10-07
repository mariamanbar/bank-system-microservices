package com.mariam.customerservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mariam.customerservice.dto.LogDTO;

@FeignClient(name = "logger-service", url = "http://localhost:8086")
public interface LoggerClient {

    @PostMapping("/api/logs")
    void sendLog(@RequestBody LogDTO log);
}