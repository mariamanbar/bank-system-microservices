package com.mariam.cardservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String customerId;
    private String accountId; 
    
    private String cardType; 
    
    // Stored as "4475111122223333" (No spaces in DB)
    private String cardNumber; 
    
    private String cvv;       
    private int pin;      
    private LocalDate expiryDate;
    
    @Enumerated(EnumType.STRING)
    private cardStatus status;
    
    public enum cardStatus{
    	ACTIVE, PENDING, DECLINED
    }
}
