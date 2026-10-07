package com.mariam.accountservice.dto;

import com.mariam.accountservice.model.Account.AccountType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class LogDTO {
	private String serviceName;
    private String type;
    private String customerId;
    private String accountId; //null if irrelevant
    private String message;
    
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
}