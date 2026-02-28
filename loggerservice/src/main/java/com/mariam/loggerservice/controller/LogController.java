package com.mariam.loggerservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariam.loggerservice.model.Log;
import com.mariam.loggerservice.repository.LogRepository;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogRepository logRepository;

    @PostMapping(value = "", consumes = "application/json", produces = "application/josn")
    public void createLog(@RequestBody Log log) {
        
         logRepository.save(log);
    }
}
