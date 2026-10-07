package com.mariam.loanservice.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class FailedAction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String actionType; 
    private String payload; 
    
    private int retryCount;
    
    private LocalDateTime nextRetryAt;
    
    public FailedAction(String actionType, String payload) {
        this.actionType = actionType;
        this.payload = payload;
        this.retryCount = 0;
        nextRetryAt = LocalDateTime.now();
    }

    
}