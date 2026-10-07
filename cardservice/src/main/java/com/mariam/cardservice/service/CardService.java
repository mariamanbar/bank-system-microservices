package com.mariam.cardservice.service;

import com.mariam.cardservice.client.LoggerClient;
import com.mariam.cardservice.dto.LogDTO;
import com.mariam.cardservice.model.Card;
import com.mariam.cardservice.model.Card.cardStatus;
import com.mariam.cardservice.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;
    
    @Autowired
	private LoggerClient loggerClient;

    private final Random random = new Random();

    /*
     * issue a card
     * return
     */
    public Card issueCard(String customerId, String accountId, int pin, String type) {
        

        StringBuilder number = new StringBuilder("44752275");
        for (int i = 0; i <8; i++) {
            number.append(random.nextInt(10));
        }
        int cvv = 100 + random.nextInt(900); 

        Card card =  Card.builder().customerId(customerId)
        		.accountId(accountId).cardType(type)
        		.pin(pin).expiryDate(LocalDate.now().plusYears(5))
        		.cvv(String.valueOf(cvv))
        		.cardNumber(number.toString())
        		.status(cardStatus.ACTIVE)
        		.build();

        try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Card-Service")
                .type("GENERAL")
                .accountId(card.getAccountId())
                .customerId(card.getCustomerId())
                .message("Customer ID " + customerId + " issued a Card. ")
                .build());
        } catch (Exception e) {
            System.err.println("Logger down, but card was deleted: " + e.getMessage());
        }
        
        return cardRepository.save(card);
    }
    
    public java.util.List<Card> getCustomerCards(Long customerId) {
        return cardRepository.findByCustomerId(customerId);
    }
    
    public List<Card>getAllCards(){
    	return cardRepository.findAll();
    }
    
    public void updateStatus(int id, Card.cardStatus newStatus) {
        // Find the card by ID
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found with ID: " + id));

        card.setStatus(newStatus);
        
        cardRepository.save(card);
        
        
        try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Card-Service")
                .type("GENERAL")
                .accountId(card.getAccountId())
                .customerId(card.getCustomerId())
                .message("Card ID " + id + " status was updated. ")
                .build());
        } catch (Exception e) {
            System.err.println("Logger down, but card was deleted: " + e.getMessage());
        }
    }
    
    //delete
    public void deleteCard(int id) {
        // 1. Check if card exists
        if (!cardRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete: Card ID " + id + " not found.");
        }

        Optional<Card> cardOpt = cardRepository.findById(id);
        Card card = cardOpt.get();
        
        // 2. Perform deletion
        cardRepository.deleteById(id);
        
        try {
            loggerClient.sendLog(LogDTO.builder()
                .serviceName("Card-Service")
                .type("GENERAL")
                .accountId(card.getAccountId())
                .customerId(card.getCustomerId())
                .message("Card ID " + id + " was permanently revoked/deleted.")
                .build());
        } catch (Exception e) {
            System.err.println("Logger down, but card was deleted: " + e.getMessage());
        }

        
    }
}
