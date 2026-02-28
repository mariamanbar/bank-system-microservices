package com.mariam.cardservice.dto;

import com.mariam.cardservice.model.Card;

import lombok.Data;

@Data
public class CardDTO {

	private String customerId;
    private String accountId; 
    
    private String cardType; 
    private int pin;     
    
    public static Card toEntity(CardDTO dto) {
    	Card card = Card.builder().customerId(dto.customerId)
    			.accountId(dto.accountId).cardType(dto.cardType).pin(dto.pin)
    			.build();
        
        return card;
    }
	
	
	public static CardDTO fromEntity(Card card) {
        CardDTO dto = new CardDTO();
        dto.setAccountId(card.getAccountId());
        dto.setCustomerId(card.getCustomerId());
        dto.setCardType(card.getCardType());
        dto.setPin(card.getPin());
        
        return dto;
    }
}
