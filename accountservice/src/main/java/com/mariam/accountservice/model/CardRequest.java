package com.mariam.accountservice.model;

import lombok.Data;

@Data
public class CardRequest {

	private String customerId;
    private String accountId; 
    
    private String cardType; 
    private int pin;
}
