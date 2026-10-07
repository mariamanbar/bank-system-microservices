package com.mariam.loggerservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;

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
    
    @GetMapping(value = "", produces = "application/json")
    @CrossOrigin(origins = "http://127.0.0.1:5500") // Enable CORS for your Live Server
    public List<Log> getAllLogs() {
        // Returns all logs sorted by timestamp descending
        return logRepository.findAll(); 
    }
}
