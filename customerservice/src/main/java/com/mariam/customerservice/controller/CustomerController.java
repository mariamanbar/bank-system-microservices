package com.mariam.customerservice.controller;

import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mariam.customerservice.dto.CustomerDTO;
import com.mariam.customerservice.model.Customer;
import com.mariam.customerservice.model.LoginRequest;
import com.mariam.customerservice.model.RegisterationRequest;
import com.mariam.customerservice.service.CustomerService;
import com.mariam.customerservice.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
	
	@Autowired
	CustomerService customerService;
	
	@Autowired
	JwtUtil jwtUtil;
	
//	@Autowired
//    private LoanClient loanClient;
	
	
//	/*
//	 * Apply for a loan
//	 */
//	@PostMapping("/apply-loan")
//	public ResponseEntity<?> applyForLoan(@RequestParam int id, @RequestParam float amount) {
//	    try {
//	        
//	        LoanRequest request = new LoanRequest(id, amount);
//	        LoanResponse response = loanClient.requestLoan(request);
//	        
//	        return ResponseEntity.ok(response);
//	    } catch (Exception e) {
//	        log.error("Error calling Loan Service: ", e);
//	        return ResponseEntity.status(500).body("Loan Service is currently unavailable.");
//	    }
//	}
	
	/*
	 * Create Customer
	 */
	@PostMapping(value = "/register", consumes = "application/json", produces="application/json")
	public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody RegisterationRequest registerationRequest){
		Boolean savedCustomer = customerService.createCustomer(registerationRequest);
		Map<String, Object> response = new LinkedHashMap<>();
		if (savedCustomer == null) {
		    response.put("message", "Service temporarily unavailable (circuit breaker)");
		    return ResponseEntity.status(503).body(response);
		}
		if(!savedCustomer) {
			response.put("message", "Email is already taken!");
			return ResponseEntity.badRequest().body(response);
		}else {
			response.put("message", "Customer created successfully");
			return ResponseEntity.ok(response);
		}
		
	}
	
	
	/*
	 Login
	 */
	@PostMapping(value ="/login", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest){
		
		Optional<Customer> optCustomer = customerService.getCustomerByEmail(loginRequest.getEmail());
		
		Map<String, Object> response = new LinkedHashMap<>();
		
		if(optCustomer.isPresent()) {
			Customer customer = optCustomer.get();
			
			if(customer.getPassword().equals(loginRequest.getPassword())) {
				String token = jwtUtil.generateToken(customer.getEmail());
				
				response.put("message", "Login successful");
                response.put("token", token);
                return ResponseEntity.ok(response);
			}
		}
		
		response.put("message", "Invalid email or password");
        return ResponseEntity.status(401).body(response);
	}
	
	
	/*
	 Get All Customers
	 Get Rich Customers
	 Get Customer By Id
	 */
	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<Map<String, Object>> getAllCustomers(@RequestParam(required = false) String id, @RequestParam(required = false)  Float  amount){
		if (id != null) {
			Optional<Customer> customer = customerService.getCustomerById(id);
			Map<String, Object> response = new LinkedHashMap<>();
			
			if(!customer.isPresent()) {
				response.put("message", "Customer not found!");
				return ResponseEntity.status(404).body(response); 
			}
			
			response.put("message", "Customer found!");
			response.put("data", CustomerDTO.fromEntity(customer.get()));
			return ResponseEntity.ok(response);
		} else if(amount != null) {
			List<Customer> richCustomers = customerService.getRichCustomers(amount);
	        
	        List<CustomerDTO> customersDto = new ArrayList<>();
	        for (Customer c : richCustomers) {
	            customersDto.add(CustomerDTO.fromEntity(c));
	        }

	        
	        Map<String, Object> response = new LinkedHashMap<>();
	        if (customersDto.isEmpty()) {
	             response.put("message", "No customers found with balance > " + amount);
	        } else {
	             response.put("message", "Found " + customersDto.size() + " rich customers!");
	             response.put("data", customersDto);
	        }
	        
	        return ResponseEntity.ok(response);
		} else {
			List<Customer> customers = customerService.getAllCustomers();
			
	        List<CustomerDTO> customersDto = new ArrayList<>();
	        for (Customer c : customers) {
	            customersDto.add(CustomerDTO.fromEntity(c));
	        }
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("message", "List of All Customers"); 
		    response.put("data", customersDto);
			return ResponseEntity.ok(response);	
		}
	}
	
	
	/*
	 * Update Customer by Id
	 */
	@PutMapping(value = "", consumes = "application/json", 
		    produces = "application/json")
    public ResponseEntity<Map<String, Object>> updateCustomer(@RequestBody Customer customer){
		
		boolean updatedCustomer = customerService.updateCustomer(customer);
		Map<String, Object> response = new LinkedHashMap<>();
		if(!updatedCustomer) {
			response.put("message", "Customer not Found!");
			return ResponseEntity.status(404).body(response);
		}else {
			response.put("message", "Customer updated successfully");
			return ResponseEntity.ok(response);
		}
		
	}
	
	
	/*
	  Delete Customer By Id
	 */
	@DeleteMapping(value = "", produces = "application/json")
	public ResponseEntity<Map<String, Object>> deleteCustomer(@RequestParam String id){

		Map<String, Object> response = new LinkedHashMap<>();
		
		Optional<Customer> customer = customerService.getCustomerById(id);
		if(!customer.isPresent()) {
			response.put("message", "Customer not found!");
			return ResponseEntity.status(404).body(response); 
		}
		customerService.deleteCustomer(id);
		response.put("message", "Customer Deleted!");
		response.put("data", customer.get());	
		return ResponseEntity.ok(response);
	}
	
	
	
}
