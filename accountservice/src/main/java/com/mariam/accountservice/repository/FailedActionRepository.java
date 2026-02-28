package com.mariam.accountservice.repository;

import com.mariam.accountservice.model.FailedAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FailedActionRepository extends JpaRepository<FailedAction, Long> {

    List<FailedAction> findByActionType(String actionType);

    List<FailedAction> findByNextRetryAtBefore(LocalDateTime time);
}
