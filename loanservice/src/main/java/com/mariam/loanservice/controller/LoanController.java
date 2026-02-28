package com.mariam.loanservice.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mariam.loanservice.model.DelayRequest;
import com.mariam.loanservice.model.Loan;
import com.mariam.loanservice.model.LoanRequest;
import com.mariam.loanservice.model.PaymentRequest;
import com.mariam.loanservice.service.LoanService;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

	@Autowired
    private LoanService loanService;
	
	/*
	 * Issue a new Loan
	 * return
	 */
	@PostMapping(value = "request", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Loan> requestLoan(@RequestBody LoanRequest request) {
		
		
		Loan  newLoan = loanService.issueLoan(request.customerId, request.amount, request.installmentAmount, request.loanType, request.timestamp);
		return ResponseEntity.ok(newLoan);
    }
	
	/*
	 * Pay an installment
	 * return
	 */
    @PostMapping(value = "/pay", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> payInstallment(@RequestBody PaymentRequest request) {
    	int result = loanService.payInstallment(request.loanId, request.amount);
    	Map<String, Object> response = new LinkedHashMap<>();
    	if(result == -1) {
    		response.put("message", "Customer not found!");
			return ResponseEntity.status(404).body(response);
    	} else if (result == 0) {
    		response.put("message", "No active loans!");
    		return ResponseEntity.badRequest().body(response);
    	} else {
    		response.put("message", "Installment was paid!");
            return ResponseEntity.ok(response);
    	}
    }
    
    
    /*
     * Delay an installment
     * Returns: JSON with message
     */
    @PostMapping(value = "/delay", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> delayPayment(@RequestBody DelayRequest request) {
        
        int result = loanService.delayInstallment(request.loanId);
        
        Map<String, Object> response = new LinkedHashMap<>();
        
        if (result == -1) {
            response.put("message", "Loan not found!");
            return ResponseEntity.status(404).body(response);
        } else if (result == 0) {
            response.put("message", "Loan is already paid off, cannot delay!");
            return ResponseEntity.badRequest().body(response);
        } else {
            // Success case
            response.put("message", "Payment delayed successfully. Interest rate increased.");
            return ResponseEntity.ok(response);
        }
    }
    

    /*
     * Get all loans for a customer
     */
    @GetMapping(value = "", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getLoans(@RequestParam String customerId) {
    	
    	Map<String, Object> response = new LinkedHashMap<>();
    	List<Loan> customerLoans = loanService.getCustomerLoans(customerId);
    	int numOfLoans = customerLoans.size();
    	float sumOfLoans =0;
    	for(Loan l : customerLoans) {
    		sumOfLoans += l.getRemainingAmount();
    	}
    	response.put("Number of Loans for customer is: ", numOfLoans);
    	response.put("Total amount of Loans for customer is: ", sumOfLoans);
        return ResponseEntity.ok(response);
    }
    
    
	
	
	
	
}
