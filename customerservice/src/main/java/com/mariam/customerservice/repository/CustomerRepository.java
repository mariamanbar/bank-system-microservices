package com.mariam.customerservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mariam.customerservice.model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String>{

	@Query("SELECT count(c) > 0 FROM Customer c WHERE LOWER(c.email) = LOWER(?1)")
	boolean existsByEmail(String email);

	Optional<Customer> findByEmail(String email);
	
	// Find customers with balance greater than X
    @Query("SELECT c FROM Customer c WHERE c.balance > :balance")
    List<Customer> findRichCustomers(@Param("balance") float balance);

}
