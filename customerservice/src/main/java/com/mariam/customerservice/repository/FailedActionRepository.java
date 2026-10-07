package com.mariam.customerservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mariam.customerservice.model.FailedAction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FailedActionRepository extends JpaRepository<FailedAction, Long> {

    List<FailedAction> findByActionType(String actionType);
    // Example: "Give me all failed CREATE_ACCOUNT jobs"
    
    List<FailedAction> findByNextRetryAtBefore(LocalDateTime time);


}
