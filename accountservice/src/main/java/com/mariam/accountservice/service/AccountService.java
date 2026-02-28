package com.mariam.accountservice.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mariam.accountservice.client.CardClient;
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
	private CardClient cardClient;
	
	@Autowired
	private FailedActionRepository failedActionRepository;
	
	@Autowired
	private IdempotencyRepository idempotencyRepository;
	
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
            System.err.println("Failed to send log: " + e.getMessage());
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
        	cardReq.setAccountId(account.getCustomerId());
        	cardReq.setCustomerId(account.getCustomerId());
        	cardReq.setPin(pin);
        	cardReq.setCardType(cardType);
        	
            cardClient.issueCard(cardReq);
            
        } catch (Exception e) {
            System.err.println("Card Service is down: " + e.getMessage());
            String payload = accountId + "," + String.valueOf(pin) + "," +  cardType;
            failedActionRepository.save(new FailedAction(
                    "RETRY_CARD_ISSUANCE", payload));
        }
    } 
	
	
	@Transactional
	public void creditDefaultAccount(String customerId, float amount, String timestamp) {
		String key = "CREDIT_" + timestamp + "_" + customerId + "_CURRENT";
		if (idempotencyRepository.existsById(key)) {
	        log.warn("Duplicate Debit Request blocked: {}", key);
	        return;
	    }
		idempotencyRepository.save(new IdempotencyKey(key, LocalDateTime.now()));
		
        Account account = accountRepository.findByCustomerIdAndType(customerId, AccountType.CURRENT)
                .orElseThrow(() -> new RuntimeException("No Current Account found for Customer " + customerId));

        credit(account.getAccountId(), amount);
    }
	
	
	@Transactional
	public void debitDefaultAccount(String customerId, float amount, String timestamp) {
		String key = "DEBIT_" + timestamp + "_" + customerId + "_CURRENT";
		if (idempotencyRepository.existsById(key)) {
	        log.warn("Duplicate Debit Request blocked: {}", key);
	        return;
	    }
		idempotencyRepository.save(new IdempotencyKey(key, LocalDateTime.now()));
		
        Account account = accountRepository.findByCustomerIdAndType(customerId, AccountType.CURRENT)
                .orElseThrow(() -> new RuntimeException("No Current Account found for Customer " + customerId));

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
        }

	    return true;
	}
	
}
