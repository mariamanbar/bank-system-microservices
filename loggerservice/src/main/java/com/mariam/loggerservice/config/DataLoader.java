package com.mariam.loggerservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mariam.loggerservice.model.Log;
import com.mariam.loggerservice.repository.LogRepository;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner loadData(LogRepository repository) {
        return args -> {
        };
    }
}