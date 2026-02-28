package com.mariam.loanservice.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int loanId;
	
	private String customerId;
	private float principalAmount;  // Original loan amount
	private float remainingAmount;  // How much is left to pay
	private float interestRate;
	private LocalDate nextInstallmentDate;
	private float installmentAmount;
	
	
	@Enumerated(EnumType.STRING)
    private LoanStatus status;
	
	public enum LoanStatus {
	    ACTIVE, PAID, OVERDUE
	}
	
	@Enumerated(EnumType.STRING)
    private LoanType loanType;
	
	public enum LoanType {
	    INSTANT(0.02f),      // 2%
	    CAR(0.10f),          // 10%
	    HOUSING(0.05f),      // 5%
	    COOL_GIRL(0.20f);    // 20%

	    private final float interestRate;

	    LoanType(float interestRate) {
	        this.interestRate = interestRate;
	    }

	    public float getInterestRate() {
	        return interestRate;
	    }
	}
	
}
