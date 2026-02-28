package com.mariam.cardservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mariam.cardservice.model.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, Integer>{

	List<Card> findByCustomerId(Long customerId);
}
