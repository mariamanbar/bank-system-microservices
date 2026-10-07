package com.mariam.loanservice.repository;


import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mariam.loanservice.model.Loan;
import com.mariam.loanservice.model.Loan.LoanStatus;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Integer> {
    List<Loan> findByCustomerId(String customerId);

    List<Loan> findByStatusAndNextInstallmentDateLessThanEqual(LoanStatus status, LocalDate date);
}
