package com.mariam.loanservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mariam.loanservice.client.LoggerClient;
import com.mariam.loanservice.dto.LogDTO;
import com.mariam.loanservice.model.FailedAction;
import com.mariam.loanservice.model.IdempotencyKey;
import com.mariam.loanservice.model.Loan;
import com.mariam.loanservice.model.Loan.LoanStatus;
import com.mariam.loanservice.model.Loan.LoanType;
import com.mariam.loanservice.repository.FailedActionRepository;
import com.mariam.loanservice.repository.IdempotencyRepository;
import com.mariam.loanservice.repository.LoanRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@Slf4j
public class LoanService {

	@Autowired
    private LoanRepository loanRepository;
	
	@Autowired
	private FailedActionRepository failedActionRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate; // To talk to Account Service

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
	private LoggerClient loggerClient;
    
    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    
    /*
     * Issue a Loan
     * return
     */
    public Loan issueLoan(String customerId, float principalAmount, float installmentAmount, LoanType type, String timeStamp) {
    	
    	String key = "ISSUE_LOAN_" + timeStamp + "_" + customerId;

        // 2. Check & Block
        if (idempotencyRepository.existsById(key)) {
            throw new RuntimeException("Duplicate Loan Application!"); 
        }
        
        // 3. Save Key
        idempotencyRepository.save(new IdempotencyKey(key, LocalDateTime.now()));
        
    	float rate = type.getInterestRate();
        float interestAmount = principalAmount * rate;
        float totalOwed = principalAmount + interestAmount;
        Loan loan = Loan.builder()
                .customerId(customerId)
                .principalAmount(principalAmount)
                .remainingAmount(totalOwed) 
                .interestRate(interestAmount)
                .nextInstallmentDate(LocalDate.now().plusMonths(1))
                .status(LoanStatus.ACTIVE)
                .loanType(type)
                .installmentAmount(installmentAmount)
                .build();

        Loan savedLoan = loanRepository.save(loan);

        // Notify Account Service to DEPOSIT the money
        sendToAccountService(customerId, principalAmount, type,"LOAN_DISBURSED");
        
        try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Loan-Service")
                .type("GENERAL")
                .customerId(String.valueOf(customerId)) 
                .accountId(null) // TODO
                .message("Customer " + customerId + " Issued a loan")
                .build());
        } catch (Exception e) {
            // Prevent app crash if Logger Service is down
            System.err.println("Failed to send log: " + e.getMessage());
        }

        return savedLoan;
        
    }
    
    /*
     * Pay  Installment
     * return
     */
    public int payInstallment(int loanId, float amount) {
        Optional<Loan> optloan = loanRepository.findById(loanId);
        
        if(!optloan.isPresent()) {
        	return -1;  // Loan not found!
        }
         Loan loan = optloan.get();
        
        if (loan.getStatus() == LoanStatus.PAID) {
            return 0; // Loan is paid!
        } 

        // Deduct from remaining balance
        float newRemainingAmount = loan.getRemainingAmount() + amount;
        
        if (newRemainingAmount <= 0) {
            loan.setRemainingAmount(0);
            loan.setStatus(LoanStatus.PAID);
        } else {
            loan.setRemainingAmount(newRemainingAmount);
            // Move next date by 1 month
            loan.setNextInstallmentDate(loan.getNextInstallmentDate().plusMonths(1));
        }
        
        loanRepository.save(loan);

        // Notify Account Service to WITHDRAW the money
        sendToAccountService(loan.getCustomerId(), amount, loan.getLoanType(),"LOAN_PAYMENT");
        
        try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Loan-Service")
                .type("TRANSACTIONAL")
                .customerId(String.valueOf(loan.getCustomerId())) 
                .accountId(null) // No account here yet
                .message("Customer " + loan.getCustomerId() + " payed installment for loan: " + loanId)
                .build());
        } catch (Exception e) {
            // Prevent app crash if Logger Service is down
            System.err.println("Failed to send log: " + e.getMessage());
        }

        return 1;  // Payment done successfully! 
    }
    
    @Transactional // Ensures DB integrity
    public void chargeInstallment(Loan loan) {
        
        // 1. Calculate Amount (Don't charge more than they owe)
        // Example: owe 200, installment is 500 -> Charge 200.
        float amountToCharge = Math.min(loan.getInstallmentAmount(), loan.getRemainingAmount());

        // 2. Update the Loan Entity
        float newRemaining = loan.getRemainingAmount() - amountToCharge;
        loan.setRemainingAmount(newRemaining);

        if (newRemaining <= 0.01) {
            loan.setRemainingAmount(0);
            loan.setStatus(LoanStatus.PAID);
            loan.setNextInstallmentDate(null);
        } else {
            // Move next date by 1 month
            loan.setNextInstallmentDate(loan.getNextInstallmentDate().plusMonths(1));
        }

        loanRepository.save(loan);

        // 3. Construct JSON for Account Service
        JSONObject json = new JSONObject();
        json.put("type", "LOAN_PAYMENT");
        json.put("customerId", loan.getCustomerId());
        json.put("amount", amountToCharge);
        
        
        // 4. Send to RabbitMQ (With Retry Logic)
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, json.toString());
            log.info("Charged installment of {} for Loan {}", amountToCharge, loan.getLoanId());

        } catch (Exception e) {
            log.error("RabbitMQ failed for Loan {}. Saving to Retry...", loan.getLoanId());
            
            // Save to FailedAction so the Retry Scheduler can fix it later
            failedActionRepository.save(new FailedAction(
                "LOAN PAYMENT", 
                json.toString()
            ));
        }
    }

    
    /*
     * Delay Installment
     * return
     */
    public int delayInstallment(int loanId) {
    	
        Optional<Loan> optloan = loanRepository.findById(loanId);
        
        if(!optloan.isPresent()) {
        	return -1;  // Loan not found!
        }
         Loan loan = optloan.get();
        
        if (loan.getStatus() == LoanStatus.PAID) {
            return 0; // Loan is paid!
        } 
        
        // Penalty: Increase interest rate by 1.5%
        float newRate = (float) (loan.getInterestRate() + 1.5);
        loan.setInterestRate(newRate);
        
        // Recalculate remaining amount with penalty (simplified logic)
        float penaltyAmount = (float) (loan.getRemainingAmount() * 0.015); 
        loan.setRemainingAmount(loan.getRemainingAmount() + penaltyAmount);

        // Push the date back by 1 month
        loan.setNextInstallmentDate(loan.getNextInstallmentDate().plusMonths(1));

        loanRepository.save(loan);
        
        try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Loan-Service")
                .type("GENERAL")
                .customerId(String.valueOf(loan.getCustomerId())) 
                .accountId(null) // No account yet
                .message("Customer " + loan.getCustomerId() + " delayed an installment for loan: " + loanId)
                .build());
        } catch (Exception e) {
            // Prevent app crash if Logger Service is down
            System.err.println("Failed to send log: " + e.getMessage());
        }
        
        return 1;
    }
    
    /*
     * Get Customer Loans
     * return
     */
    public List<Loan> getCustomerLoans(String customerId) {
        return loanRepository.findByCustomerId(customerId);
    } 
    
    
    /*
     * Helper to send MQ messages
     */
    private void sendToAccountService(String customerId, float amount, LoanType loanType, String eventType) {
        try {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("customerId", customerId);
            json.put("amount", amount);
            json.put("type", eventType); // "LOAN_DISBURSED" or "LOAN_PAYMENT"
            
            if (loanType != null) {
                json.put("loanType", loanType.name());
            }
            
            rabbitTemplate.convertAndSend(exchange, routingKey, json.toString());
        } catch (Exception e) {
            System.err.println("Failed to send MQ message: " + e.getMessage());
        }
    }
    
}
