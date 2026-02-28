package com.mariam.accountservice.model;

import lombok.Data;

@Data
public class DepositRequest {

	public int customerId;
	public String accountId;
    public float amount;
}
