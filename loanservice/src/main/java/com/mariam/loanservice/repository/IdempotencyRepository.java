package com.mariam.loanservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mariam.loanservice.model.IdempotencyKey;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, String> {

}