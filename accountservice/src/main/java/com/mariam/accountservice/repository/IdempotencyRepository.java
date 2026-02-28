package com.mariam.accountservice.repository;

import com.mariam.accountservice.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, String> {

}
