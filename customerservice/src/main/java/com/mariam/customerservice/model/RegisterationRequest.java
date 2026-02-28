package com.mariam.customerservice.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class RegisterationRequest {
	
	
	private String name;
	
	private String email;
	
	private String password;
	
	private LocalDate dob;
	private	String phone;
	private String natID;

}

