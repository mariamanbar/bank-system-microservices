package com.mariam.accountservice.model;

import com.mariam.accountservice.model.Account.AccountType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class CreateRequest {

	private String customerId;
	
	@Enumerated(EnumType.STRING)
	private AccountType accountType;
	
}
