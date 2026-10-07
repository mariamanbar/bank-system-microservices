package com.mariam.customerservice.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.PrePersist;
import java.security.SecureRandom;



@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
	
	@Id
	private String id;
	private String name;
	
	private String email;
	private String password;
	private float balance;
	
	private String natId;
	private String phone;
	private LocalDate DOB;
	
	@PrePersist
	public void generateId() {
		if(this.id == null)
			this.id = randomId();
	}
	
	private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String NUMBERS = "0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();
	
	private String randomId() {
	    char first = LETTERS.charAt(RANDOM.nextInt(26));
	    char second = LETTERS.charAt(RANDOM.nextInt(26));
	    char digit = NUMBERS.charAt(RANDOM.nextInt(10));

	    return "" + first + second + digit; 
	}
	
	

}
