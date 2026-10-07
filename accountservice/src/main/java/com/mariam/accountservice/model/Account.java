package com.mariam.accountservice.model;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Account {

	@Id
	private String accountId;
	
	private String customerId;
	
	private Integer accountNumber;
	
	private float balance;
	
	
	@Enumerated(EnumType.STRING)
	private AccountType type;
	
	public enum AccountType {
	    LOAN,       // 100 - 199
	    DEPOSIT,    // 200 - 299
	    CURRENT,    // 500 - 525 (Default)
	    SALARY,     // 550 - 599
	    SAVINGS     // 600 - 699
	}
	
}
