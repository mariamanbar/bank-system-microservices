package com.mariam.loanservice.model;

import lombok.Data;

@Data
public class PaymentRequest {

	public int loanId;
    public float amount;
}
