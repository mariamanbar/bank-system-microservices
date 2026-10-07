package com.mariam.loggerservice.model;


import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "logs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Log {
    @Id
    private String id;
    private String serviceName; 
    private String type;        
    private String customerId;  
    private String accountId;
    private String message;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}