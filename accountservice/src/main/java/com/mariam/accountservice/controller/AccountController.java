package com.mariam.accountservice.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariam.accountservice.model.CardRequest;
import com.mariam.accountservice.model.CreateRequest;
import com.mariam.accountservice.model.DepositRequest;
import com.mariam.accountservice.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

  
    // credit (Adding)
    @PostMapping(value = "/credit", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> credit(@RequestBody DepositRequest request) {
    	boolean depositResult = accountService.credit(request.accountId, request.amount);
        Map<String, Object> response = new LinkedHashMap<>();
		if(!depositResult) {
			response.put("message", "Account not found!");
			return ResponseEntity.badRequest().body(response);
		}else {
			response.put("message", "Credit done successfully");
			return ResponseEntity.ok(response);
		}
    }
    
    // debit  (Adding)
    @PostMapping(value = "/debit", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> debit(@RequestBody DepositRequest request) {
    	boolean depositResult = accountService.debit(request.accountId, request.amount);
        Map<String, Object> response = new LinkedHashMap<>();
		if(!depositResult) {
			response.put("message", "Account not found!");
			return ResponseEntity.badRequest().body(response);
		}else {
			response.put("message", "Debit done successfully");
			return ResponseEntity.ok(response);
		}
    }
    
    /*
     * Create a new account
     */
    @PostMapping(value = "/createAccount", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody CreateRequest createRequest){
    	
    	 Map<String, Object> response = new LinkedHashMap<>();
    	 accountService.createAccount(createRequest.getCustomerId(), createRequest.getAccountType(), 0);
    	 
    	 response.put("message", "Account Created successfully");
		 return ResponseEntity.ok(response);
    }
    
    /*
     * issue a card
     */
    @PostMapping(value = "/createCard", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> orderCardForAccount(@RequestBody CardRequest cardRequest){
    	
    	 Map<String, Object> response = new LinkedHashMap<>();
    	 accountService.orderCardForAccount(cardRequest.getAccountId(), cardRequest.getPin(), cardRequest.getCardType());
    	 
    	 response.put("message", "Card Created successfully");
		 return ResponseEntity.ok(response);
    }
    
}