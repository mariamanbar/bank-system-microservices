package com.mariam.customerservice.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mariam.customerservice.client.LoggerClient;
import com.mariam.customerservice.dto.LogDTO;
import com.mariam.customerservice.model.FailedAction;
import com.mariam.customerservice.repository.FailedActionRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CustomerJobScheduler {
	
	@Autowired
	private FailedActionRepository failedActionRepository;
	
	@Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private LoggerClient loggerClient;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;
	
	
    @Scheduled(fixedDelay = 60000)
    public void retryFailedJobs() {

    	List<FailedAction> failures = failedActionRepository.findByNextRetryAtBefore(LocalDateTime.now());

        if (failures.isEmpty()) {
            return; 
        }

        log.info("Scheduler found {} failed jobs. Starting retry...", failures.size());

        for (FailedAction action : failures) {
        	String jsonPayload = action.getPayload();
        	
            try {
                if ("ACCOUNT REGISTRATION".equals(action.getActionType())) {
                    
                    log.info("Retrying RabbitMQ Registration Event for Payload: {}", action.getPayload());
                    
                    rabbitTemplate.convertAndSend(exchange, routingKey, jsonPayload);
                    
                    log.info("Retry SUCCESS: MQ message sent.");
                    
                    failedActionRepository.delete(action);
                } else if ("SEND REGISTRATION LOG".equals(action.getActionType())) {
                    
                    log.info("Retrying Logger Service Call for: {}", action.getPayload());

                    JSONObject json = new JSONObject(jsonPayload);
                    String customerId = json.getString("customerId");
                    String email = json.getString("email");
                    
                    LogDTO logDto = LogDTO.builder()
                    		.serviceName("Customer-Service")
                    		.type("GENERAL")
                            .customerId(customerId) 
                            .accountId(null) 
                            .message("New customer registered: " + email)
                            .build();
                    		 
                    loggerClient.sendLog(logDto);
                    
                    log.info("Retry SUCCESS: Log sent.");
                    failedActionRepository.delete(action);
                }

            } catch (Exception e) {
                // If it fails AGAIN, we update the retry count
                log.warn("Retry failed for Job ID {}: {}", action.getId(), e.getMessage());
                handleFailure(action);
            }
        }
    }
    
    private void handleFailure(FailedAction action) {
    	
        int newCount = action.getRetryCount() + 1;
        action.setRetryCount(newCount);

        if (newCount >= 5) {
            log.error("Job {} failed 5 times. Deleting...", action.getId());
            failedActionRepository.delete(action);
        } else {
        	long delayMinutes = (long) Math.pow(2, newCount);
        	action.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            failedActionRepository.save(action);
        }
    }
	    
}
