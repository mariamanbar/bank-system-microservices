package com.mariam.accountservice.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mariam.accountservice.model.FailedAction;
import com.mariam.accountservice.model.Account.AccountType;
import com.mariam.accountservice.repository.FailedActionRepository;
import com.mariam.accountservice.service.AccountService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AccountJobScheduler {

    @Autowired
    private FailedActionRepository failedActionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000) // Run every 1 minute
    public void retryFailedTransactions() {
        List<FailedAction> failures = failedActionRepository.findByNextRetryAtBefore(LocalDateTime.now());

        if (failures.isEmpty()) return;

        log.info("Retrying {} failed account transactions...", failures.size());

        for (FailedAction action : failures) {
            try {
                JsonNode json = objectMapper.readTree(action.getPayload());
                String customerId = json.get("customerId").asText();
                float amount = json.get("amount").floatValue();
                String timestamp = json.get("timestamp").asText();
                

                if ("RETRY_DEBIT".equals(action.getActionType())) {
                    log.info("Retrying DEBIT for Customer {}", customerId);
                    accountService.debitDefaultAccount(customerId, amount, timestamp);   //have timestamp
                    
                } else if ("RETRY_CREDIT".equals(action.getActionType())) {
                    log.info("Retrying CREDIT for Customer {}", customerId);
                    accountService.creditDefaultAccount(customerId, amount, timestamp);  //have timestamp
                    
                } else if("RETRY_CREATION".equals(action.getActionType())) {
                	log.info("Retrying REGISTRATION for Customer: {}", customerId);
                    accountService.createAccount(customerId, AccountType.CURRENT, 0);
                    
                } else if("RETRY_LOAN_ACCOUNT_CREATION".equals(action.getActionType())) {
                	log.info("Retrying REGISTRATION for Customer: {}", customerId);
                    accountService.createAccount(customerId, AccountType.LOAN, 0);
                } 
                else if("RETRY_CARD_ISSUANCE".equals(action.getActionType())) {
                	log.info("Retrying CARD Isuance for Customer: {}", customerId);
                	String payload = action.getPayload();
                	String[] parts = payload.split(",");
                    accountService.orderCardForAccount(parts[0], Integer.valueOf(parts[1]), parts[3]);
                }

                log.info("Retry SUCCESS. Deleting record.");
                failedActionRepository.delete(action);

            } catch (Exception e) {
                log.error("Retry failed again for ID {}: {}", action.getId(), e.getMessage());
                handleFailure(action);
            }
        }
    }

    private void handleFailure(FailedAction action) {
        int newCount = action.getRetryCount() + 1;
        action.setRetryCount(newCount);
        
        if (newCount >= 5) {
            log.error("Transaction {} failed 5 times. Manual intervention needed.", action.getId());
            failedActionRepository.delete(action); 
        } else {
            long delay = (long) Math.pow(3, newCount); 
            action.setNextRetryAt(LocalDateTime.now().plusMinutes(delay));
            failedActionRepository.save(action);
        }
    }
}
