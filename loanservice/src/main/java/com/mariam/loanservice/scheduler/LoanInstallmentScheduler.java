package com.mariam.loanservice.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mariam.loanservice.model.FailedAction;
import com.mariam.loanservice.model.Loan;
import com.mariam.loanservice.model.Loan.LoanStatus;
import com.mariam.loanservice.repository.FailedActionRepository;
import com.mariam.loanservice.repository.LoanRepository;
import com.mariam.loanservice.service.LoanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanInstallmentScheduler {

	@Autowired
    private LoanRepository loanRepository;
	
	@Autowired
	private LoanService loanService;
    
    @Autowired
    private FailedActionRepository failedActionRepository;

    @Scheduled(cron = "0 0 2 * * *") // every day at 2:00 AM
    public void processInstallments() {

        LocalDate today = LocalDate.now();
        List<Loan> dueLoans = loanRepository
                .findByStatusAndNextInstallmentDateLessThanEqual(LoanStatus.ACTIVE, today);

        if (dueLoans.isEmpty()) return;

        log.info("Found {} loans with due installments (<= {}).", dueLoans.size(), today);

        for (Loan loan : dueLoans) {
            try {
                // Call the SERVICE to do the logic
                loanService.chargeInstallment(loan);
                
            } catch (Exception e) {
                log.error("Error processing Loan ID: {}. Error: {}", loan.getLoanId(), e.getMessage());
                // Since this loop is inside the Scheduler, one failure won't stop the others.
                saveFailedInstallment(loan, e.getMessage());
            }
        }
    }
    
    
    private void saveFailedInstallment(Loan loan, String error) {
        JSONObject payload = new JSONObject();
        payload.put("loanId", loan.getLoanId());
        payload.put("customerId", loan.getCustomerId());
        payload.put("amount", Math.min(loan.getInstallmentAmount(), loan.getRemainingAmount()));
        payload.put("type", "LOAN_INSTALLMENT");

        FailedAction fa = new FailedAction();
        fa.setActionType("CHARGE_INSTALLMENT");
        fa.setPayload(payload.toString());
        fa.setRetryCount(0);
        fa.setNextRetryAt(LocalDateTime.now().plusMinutes(5)); 
        failedActionRepository.save(fa);
    }

}

