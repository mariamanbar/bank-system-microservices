package com.mariam.customerservice.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.mariam.customerservice.client.LoggerClient;
import com.mariam.customerservice.dto.LogDTO;
import com.mariam.customerservice.model.Customer;
import com.mariam.customerservice.model.FailedAction;
import com.mariam.customerservice.model.LoanRequest;
import com.mariam.customerservice.model.LoanResponse;
import com.mariam.customerservice.model.RegisterationRequest;
import com.mariam.customerservice.repository.CustomerRepository;
import com.mariam.customerservice.repository.FailedActionRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;



@Service
@Slf4j
public class CustomerService {
	
	@Autowired
	CustomerRepository customerRepository;
	
	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	@Autowired
	private LoggerClient loggerClient;
	
	@Autowired
	private FailedActionRepository failedActionRepository;

	@Value("${rabbitmq.exchange.name}")
	private String exchange;

	@Value("${rabbitmq.routing.key}")
	private String routingKey;
	
	
	
	/*
	 * Get All Customers
	 * return
	 */
	public List<Customer> getAllCustomers(){
		return customerRepository.findAll();
	}
	
	
	/*
	 * Find Customer By Id
	 * return
	 */
	public Optional<Customer> getCustomerById(String id) {
		
		Optional<Customer> customer = customerRepository.findById(id);
		
        return customer;
    }
	
	/*
	 * Find Customer By email
	 * Login
	 * return
	 */
	public Optional<Customer> getCustomerByEmail(String email){
		 
		return customerRepository.findByEmail(email);
	 }
	

	/*
	 * Create Customer
	 * Register
	 * return
	 */
	@CircuitBreaker(name = "CreateCustomerCircuit", fallbackMethod = "CreateCustomerFallback")
	public Boolean createCustomer(RegisterationRequest registerationRequest) {
		String email = registerationRequest.getEmail();
//		if (customerRepository.existsByEmail(email)) {
//            return false; 
//        }
		if (customerRepository.existsByEmail(email)) {
             throw new RuntimeException("Service is down");
        }
		String pass = registerationRequest.getPassword();
		String name = registerationRequest.getName();
		LocalDate dob = registerationRequest.getDob();
		String phone = registerationRequest.getPhone();
		String natID = registerationRequest.getNatID();
		
		Customer newCustomer = Customer.builder()
				.email(email).password(pass).balance(0).name(name)
				.DOB(dob).phone(phone).natId(natID)
				.build();
		Customer saved = customerRepository.save(newCustomer);
		
		JSONObject json = new JSONObject();
        json.put("customerId", saved.getId());
        json.put("name", saved.getName());
        json.put("email", saved.getEmail());
        json.put("type", "REGISTRATION_EVENT");
        
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, json.toString());
            log.info("Sent Registration MQ Message: {}", json);
            
        } catch (Exception e) {
            log.error("RabbitMQ is down! Saving to retry later...");
            
            // Save to FailedAction so the Scheduler can resend this JSON later
            failedActionRepository.save(new FailedAction(
                "ACCOUNT REGISTRATION", 
                json.toString()
            ));
        }
        
        log.info("Sending message : {}", json);
        
        try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Customer-Service")
                .type("GENERAL")
                .customerId(newCustomer.getId()) 
                .accountId(null) // No account here yet //TODO
                .message("New customer registered: " + newCustomer.getEmail())
                .build());
            log.info("Log is sent");
        } catch (Exception e) {
        	log.error("Logger Service is down! Saving to retry later...");
       	    failedActionRepository.save(new FailedAction(
        		"SEND REGISTRATION LOG", json.toString()));
        }
        
        return true;
	}
	
	private Boolean CreateCustomerFallback(RegisterationRequest registerationRequest, Throwable e) {
		return null;
	}
	
	/*
	 * Update Customer By Id
	 * return
	 */
	public boolean updateCustomer(Customer newCustomer) {

		Optional<Customer> optCustomer = customerRepository.findById(newCustomer.getId());
		
		if(optCustomer.isPresent()) {
			Customer oldCustomer = optCustomer.get();
			
		if(newCustomer.getName() != null)
			oldCustomer.setName(newCustomer.getName());
			
		if(newCustomer.getEmail() != null)
			oldCustomer.setEmail(newCustomer.getEmail());
			
		if(newCustomer.getBalance() != 0)
			oldCustomer.setBalance(newCustomer.getBalance());
			
		customerRepository.save(oldCustomer);
		return true;
		}
		
		try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Customer-Service")
                .type("GENERAL")
                .customerId(newCustomer.getId()) 
                .accountId(null) // No account here yet //TODO
                .message("Customer" + String.valueOf(newCustomer.getId()) + "updated their info ")
                .build());
        } catch (Exception e) {
            // Prevent app crash if Logger Service is down
            System.err.println("Failed to send log: " + e.getMessage());
        }
		
		return false;
	}


	/*
	 * Delete Customer by Id
	 * void
	 */
	public void deleteCustomer(String id) {
		customerRepository.deleteById(id);
		try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Customer-Service")
                .type("GENERAL")
                .customerId(String.valueOf(id)) 
                .accountId(null) // No account yet
                .message("Customer" + id + " was deleted ")
                .build());
        } catch (Exception e) {
            // Prevent app crash if Logger Service is down
            System.err.println("Failed to send log: " + e.getMessage());
        }
		
	}
	
	
	/*
	 * Find Rich Customers
	 * return
	 */
	public List<Customer> getRichCustomers(float minBalance) {
	    return customerRepository.findRichCustomers(minBalance);
	}
	
	
}
