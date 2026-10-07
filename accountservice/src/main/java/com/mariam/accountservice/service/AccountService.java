package com.mariam.accountservice.service;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mariam.accountservice.client.CardClient;
import com.mariam.accountservice.client.CustomerClient;
import com.mariam.accountservice.client.LoggerClient;
import com.mariam.accountservice.dto.LogDTO;
import com.mariam.accountservice.model.Account;
import com.mariam.accountservice.model.Account.AccountType;
import com.mariam.accountservice.model.CardRequest;
import com.mariam.accountservice.model.FailedAction;
import com.mariam.accountservice.model.IdempotencyKey;
import com.mariam.accountservice.repository.AccountRepository;
import com.mariam.accountservice.repository.FailedActionRepository;
import com.mariam.accountservice.repository.IdempotencyRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountService {

	@Autowired
	private AccountRepository accountRepository;
	
	@Autowired
	private LoggerClient loggerClient;
	
	@Autowired
	private CustomerClient customerClient;
	
	@Autowired
	private CardClient cardClient;
	
	@Autowired
	private FailedActionRepository failedActionRepository;
	
	@Autowired
	private IdempotencyRepository idempotencyRepository;

	@Value("${gateway.keys.customer}")
	private String customerServiceKey;

	@Value("${gateway.keys.card}")
	private String cardServiceKey;
	
	/*
	 * Get All Accounts
	 */
	public List<Account> getAllAccounts() {
	    return accountRepository.findAll();
	}

	/*
	 * Find Account By Id
	 */
	public Optional<Account> getAccountById(String id) {
	    return accountRepository.findById(id);
	}

	/*
	 * Find all accounts for a specific customer
	 */
	public Optional<Account> getAccountsByCustomerId(String customerId) {
	    // Note: Ensure you have findByCustomerId defined in your AccountRepository
	    return accountRepository.findByCustomerId(customerId);
	}
	
	/*
	 * Create Account
	 * return
	 */
	public void createAccount(String customerId, AccountType type, float amount) { 
		
		int min = 0;
        int max = 0;
        
		switch (type) {
        case LOAN:      min = 100; max = 199; break;
        case DEPOSIT:   min = 200; max = 299; break;
        case CURRENT:   min = 500; max = 525; break; 
        case SALARY:    min = 550; max = 599; break;
        case SAVINGS:   min = 600; max = 699; break;
        default: throw new IllegalArgumentException("Unknown Account Type");
        }
		
		// --- VALIDATION LOGIC ---
	    try {
	        // We try to fetch the customer. If it's 404, Feign throws an exception.
	        customerClient.getCustomerById(customerId, customerServiceKey);
	        log.info("Customer {} validated. Proceeding with account creation.", customerId);
	    } catch (Exception e) {
	        log.error("Validation failed for Customer ID: {}. Error: {}", customerId, e.getMessage());
	        throw new RuntimeException("Cannot create account: Customer ID '" + customerId + "' is invalid or does not exist.");
	    }
	    
		
		Integer currentMax = accountRepository.findMaxAccountNumberByCustomerIdAndType(customerId, type);
        int nextAccountNumber = (currentMax == null) ? min : currentMax + 1;
    
		if (nextAccountNumber > max) {
            throw new RuntimeException("Range full for " + type + "! Max allowed: " + max);
        }
		
		String AccountId = customerId.concat(" - " + String.valueOf(nextAccountNumber));
		
		Account newAccount = Account.builder()
				.customerId(customerId)
				.accountId(AccountId)
				.balance(amount)
				.accountNumber(nextAccountNumber)
				.build();
		newAccount.setType(type);
		
		accountRepository.save(newAccount);
		
		try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Account-Service")
                .type("GENERAL")
                .customerId(customerId) 
                .accountId(AccountId)
                .message("Customer " + customerId + " created an account")
                .accountType(type)
                .build());
        } catch (Exception e) {
            // Prevent app crash if Logger Service is down
            System.err.println("Failed to send log: " + e.getMessage() + "Saving to resend later. ");
            JSONObject json = new JSONObject();
            json.put("customerId", newAccount.getCustomerId());
            json.put("accountId", newAccount.getAccountId());
            json.put("balance", newAccount.getBalance());
            json.put("accountNumber", newAccount.getAccountNumber());
            json.put("type", "REGISTRATION_EVENT");
            failedActionRepository.save(new FailedAction(
            		"SEND REGISTRATION LOG", json.toString()));
        }
		
		
	}
	
	/*
	 * Delete Account by Id
	 */
	public void deleteAccount(String id) {
	    // 1. Delete from Repository
	    accountRepository.deleteById(id);

	    // 2. Attempt to Log the Action
	    try {
	        loggerClient.sendLog(LogDTO.builder()
	            .serviceName("Account-Service")
	            .type("GENERAL")
	            .customerId(null) // Optional: If you have customerId in the Account object, pass it here
	            .accountId(id)
	            .message("Account " + id + " was permanently deleted from the system")
	            .build());
	        log.info("Delete log sent for account: {}", id);
	    } catch (Exception e) {
	        log.error("Failed to send delete log for account {}: {}", id, e.getMessage());
	        
	        // Following your pattern of saving failed actions for retry
	        JSONObject json = new JSONObject();
	        json.put("accountId", id);
	        json.put("action", "DELETE");
	        
	        failedActionRepository.save(new FailedAction(
	            "SEND DELETE ACCOUNT LOG", 
	            json.toString()
	        ));
	    }
	}
	
	/*
	 * issue a card
	 */
//  issueCard(String customerId, String accountId, int pin, String type)
	public void orderCardForAccount(String accountId, int pin, String cardType) {
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found!"));

        try {
        	CardRequest cardReq = new CardRequest();
            cardReq.setAccountId(account.getAccountId()); 
            cardReq.setCustomerId(account.getCustomerId());
        	cardReq.setPin(pin);
        	cardReq.setCardType(cardType);
        	
            cardClient.issueCard(cardReq, cardServiceKey);
            
        } catch (Exception e) {
            System.err.println("Card Service is down: " + e.getMessage());
            String payload = accountId + "," + String.valueOf(pin) + "," +  cardType;
            failedActionRepository.save(new FailedAction(
                    "RETRY CARD ISSUANCE", payload));
        }
    } 
	
	
	@Transactional
	public void creditDefaultAccount(String customerId, float amount, String timestamp) {

        Account account = accountRepository.findByCustomerIdAndType(customerId, AccountType.CURRENT)
                .orElseThrow(() -> new RuntimeException("No Current Account found for Customer " + customerId));
        
		String key = "CREDIT_" + timestamp + "_" + customerId + "_" + account.getAccountId();
		if (idempotencyRepository.existsById(key)) {
	        log.warn("Duplicate Debit Request blocked: {}", key);
	        return;
	    }
		idempotencyRepository.save(new IdempotencyKey(key, LocalDateTime.now()));
		

        credit(account.getAccountId(), amount);
    }
	
	
	@Transactional
	public void debitDefaultAccount(String customerId, float amount, String timestamp) {

        Account account = accountRepository.findByCustomerIdAndType(customerId, AccountType.CURRENT)
                .orElseThrow(() -> new RuntimeException("No Current Account found for Customer " + customerId));
        
		String key = "DEBIT_" + timestamp + "_" + customerId + "_" + account.getAccountId();
		if (idempotencyRepository.existsById(key)) {
	        log.warn("Duplicate Debit Request blocked: {}", key);
	        return;
	    }
		idempotencyRepository.save(new IdempotencyKey(key, LocalDateTime.now()));
		

        debit(account.getAccountId(), amount);
    }
	
	
	/*
	 * Credit from Account //deposit ++
	 * boolean
	 * for controller
	 */
	@Transactional
	public boolean credit(String accountId, float amount) {
	    
	    Optional<Account> optAccount = accountRepository.findById(accountId);
	    if(!optAccount.isPresent()) {
	    	return false;
	    }
	    Account account = optAccount.get();
	    float newBalance = account.getBalance() + amount;
	    account.setBalance(newBalance);

	    accountRepository.save(account);

	    try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Account-Service")
                .type("TRANSACTIONAL")
                .customerId(String.valueOf(account.getCustomerId())) 
                .accountId(account.getAccountId())
                .message("Customer " + account.getCustomerId() + " made a CREDIT transaction")
                .build());
        } catch (Exception e) {
            System.err.println("Failed to send log: " + e.getMessage());
            String payload = accountId + "," + "amount" + amount;
            failedActionRepository.save(new FailedAction(
                    "RETRY CREDIT OPERATION", payload));
        }
	    
	    return true;
	}
	
	/*
	 * Debit  from Account --
	 * boolean
	 * for controller
	 */
	@Transactional
	public boolean debit (String accountId, float amount) {
	    Optional<Account> optAccount = accountRepository.findById(accountId);
	    if(!optAccount.isPresent()) {
	    	return false;
	    }
	    Account account = optAccount.get();
	    float newBalance = account.getBalance() - amount;
	    account.setBalance(newBalance);

	    accountRepository.save(account);
	    
	    try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Account-Service")
                .type("TRANSACTIONAL")
                .customerId(account.getCustomerId()) 
                .accountId(account.getAccountId())
                .message("Customer " + account.getCustomerId() + " made a DEBIT transaction")
                .build());
        } catch (Exception e) {
            System.err.println("Failed to send log: " + e.getMessage());
            String payload = accountId + "," + "amount" + amount;
            failedActionRepository.save(new FailedAction(
                    "RETRY DEBIT OPERATION", payload));
        }

	    return true;
	}
	
}
