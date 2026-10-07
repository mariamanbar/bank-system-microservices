package com.mariam.customerservice.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mariam.customerservice.model.Customer;

import lombok.Data;

@Data
@JsonPropertyOrder({ "id", "name", "email", "balance", "password" })
public class CustomerDTO {
	
	private String id;
	private String name;
	private String email;
	private String password;
	private float balance;


	private String natId;
	private String phone;
	private LocalDate DOB;
	

	//for Database
	public static Customer toEntity(CustomerDTO dto) {
        Customer customer = new Customer();
        
        customer.setNatId(dto.getNatId());
        customer.setPhone(dto.getPhone());
        customer.setDOB(dto.getDOB());
        
        customer.setId(dto.getId());
        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setPassword(dto.getPassword());
        customer.setBalance(dto.getBalance());
        
        return customer;
    }
	
	
	public static CustomerDTO fromEntity(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        
        dto.setNatId(customer.getNatId());
        dto.setPhone(customer.getPhone());
        dto.setDOB(customer.getDOB());
        
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setBalance(customer.getBalance());
        dto.setPassword("***");
        
        return dto;
    }
}
