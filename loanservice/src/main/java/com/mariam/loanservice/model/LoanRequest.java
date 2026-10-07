package com.mariam.loanservice.model;

import com.mariam.loanservice.model.Loan.LoanType;

import lombok.Data;

@Data
public class LoanRequest {

	public String customerId;
    public float amount;
    public float installmentAmount;
    public LoanType loanType;
    public String timestamp;
}
