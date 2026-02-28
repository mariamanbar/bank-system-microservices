package com.mariam.customerservice.model;

import lombok.Data;

@Data
public class LoanResponse {

	private Long loanId;
    private Integer customerId;
    private Double principalAmount;
    private Double remainingAmount;
    private Double interestRate;
    private String status;
}
