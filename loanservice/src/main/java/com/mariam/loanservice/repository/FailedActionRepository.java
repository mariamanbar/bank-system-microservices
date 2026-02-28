package com.mariam.loanservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mariam.loanservice.model.FailedAction;

import java.util.List;

@Repository
public interface FailedActionRepository extends JpaRepository<FailedAction, Long> {

    List<FailedAction> findByActionType(String actionType);
    // Example: "Give me all failed CREATE_ACCOUNT jobs"

}
