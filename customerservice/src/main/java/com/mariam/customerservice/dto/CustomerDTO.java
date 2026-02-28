package com.mariam.customerservice.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mariam.customerservice.model.Customer;

import lombok.Data;

@Data
@JsonPropertyOrder({ "id", "name", "email", "balance", "password" })
public class CustomerDTO {
	
	private int id;
	private String name;
	private String email;
	private String password;
	private float balance;
	

	//for Database
	public static Customer toEntity(CustomerDTO dto) {
        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setPassword(dto.getPassword());
        customer.setBalance(dto.getBalance());
        
        return customer;
    }
	
	
	public static CustomerDTO fromEntity(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setBalance(customer.getBalance());
        dto.setPassword("***");
        
        return dto;
    }
}
