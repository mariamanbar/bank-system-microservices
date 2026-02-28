package com.mariam.cardservice.controller;

import com.mariam.cardservice.dto.CardDTO;
import com.mariam.cardservice.model.Card;
import com.mariam.cardservice.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @PostMapping(value = "", consumes = "application/json", produces = "application/json")
    public CardDTO issueCard(@RequestBody CardDTO carddto) {
    	Card card = cardService.issueCard(carddto.getCustomerId(), carddto.getAccountId(), carddto.getPin(),carddto.getCardType());

        return CardDTO.fromEntity(card);
    }

    @GetMapping(value = "")
    public List<Card> getCards(@RequestParam Long id) {
        return cardService.getCustomerCards(id);
    }
}