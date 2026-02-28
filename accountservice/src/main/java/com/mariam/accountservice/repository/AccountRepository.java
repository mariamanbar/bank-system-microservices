package com.mariam.accountservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mariam.accountservice.model.Account;
import com.mariam.accountservice.model.Account.AccountType;

public interface AccountRepository extends JpaRepository<Account, String>{

	Optional<Account> findByCustomerId(int customerId);
	
	@Query("SELECT MAX(a.accountNumber) FROM Account a WHERE a.customerId = :customerId AND a.type = :type")
    Integer findMaxAccountNumberByCustomerIdAndType(@Param("customerId") String customerId, @Param("type") AccountType type);
	
	Optional<Account> findByCustomerIdAndType(String customerId, AccountType type);
}
