package com.mariam.accountservice.service.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mariam.accountservice.model.Account.AccountType;
import com.mariam.accountservice.model.FailedAction;
import com.mariam.accountservice.repository.FailedActionRepository;
import com.mariam.accountservice.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class AccountMessageListener {

	@Autowired
    private AccountService accountService;
	
	@Autowired
	private FailedActionRepository failedActionRepository;
	
	@Autowired
    private ObjectMapper objectMapper;
	
	@RabbitListener(queues = "${rabbitmq.queue.name}")
	public void receiveMessage(String message) {
		try {
			
			JsonNode json = objectMapper.readTree(message);
			
			log.info("The json body is : {} ", json);
			if (!json.has("type") || !json.has("customerId")) {
                log.warn("Invalid message received: missing type or customerId");
                return;
            }
			// .path() is safer than .get()
			String eventType = json.path("type").asText();
			String customerId = json.path("customerId").asText().trim().toUpperCase();

			// Since your JSON doesn't have a timestamp, provide a default to avoid nulls
			String timestamp = json.path("timestamp").asText("no-timestamp");
			
			
			switch (eventType) {
            
            case "REGISTRATION_EVENT":
                // Create Default "Current" Account (Range 500-525)
                try {
                	log.info("Processing REGISTRATION for Customer: {}", customerId);
                    accountService.createAccount(customerId, AccountType.CURRENT, 0);
                } catch(Exception e) {
                	log.error("Registration failed! Saving to retry...", e);
                    saveFailedAction("RETRY_CREATION", message);
                }
                break;
                
            case "LOAN_DISBURSED":
            	// Use asDouble() and cast to float for 'amount'
                float loanAmount = (float) json.path("amount").asDouble();
                try {
                    log.info("Processing LOAN_DISBURSED for Customer: {}, Amount: {}", customerId, loanAmount);
                    // Use AccountType.LOAN directly since that's what your service expects
                    accountService.createAccount(customerId, AccountType.LOAN, -loanAmount);
                } catch(Exception e) {
                    log.error("Loan failed! Saving to retry...", e);
                    saveFailedAction("RETRY_LOAN_ACCOUNT_CREATION", message);
                }
                break;
			
            case "LOAN_PAYMENT":
            	float paymentAmount = json.get("amount").asFloat();
            	try {
            		log.info("Processing LOAN_PAYMENT for Customer: {}, Amount: {}", customerId, paymentAmount);
                    // Remove money from their 'Current' account
                    accountService.debitDefaultAccount(customerId, paymentAmount, timestamp);
                } catch(Exception e) {
                	log.error("Loan failed! Saving to retry...", e);
                    saveFailedAction("RETRY_DEBIT", message);
                }
                break;
                
             }

		} catch (Exception e){
			log.warn("Processing message failed : {}", e.getMessage());
			
		}
	}
	
	private void saveFailedAction(String actionType, String jsonPayload) {
        FailedAction action = new FailedAction(actionType, jsonPayload);
        failedActionRepository.save(action);
    }
		
	
}
