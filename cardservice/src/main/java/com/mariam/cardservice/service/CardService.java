package com.mariam.cardservice.service;

import com.mariam.cardservice.model.Card;
import com.mariam.cardservice.model.Card.cardStatus;
import com.mariam.cardservice.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Random;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

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

        return cardRepository.save(card);
    }
    
    public java.util.List<Card> getCustomerCards(Long customerId) {
        return cardRepository.findByCustomerId(customerId);
    }
}
