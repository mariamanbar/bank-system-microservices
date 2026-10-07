package com.mariam.cardservice.controller;

import com.mariam.cardservice.dto.CardDTO;
import com.mariam.cardservice.model.Card;
import com.mariam.cardservice.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class CardController {

    @Autowired
    private CardService cardService;

    @PostMapping(value = "", consumes = "application/json", produces = "application/json")
    public CardDTO issueCard(@RequestBody CardDTO carddto) {
    	Card card = cardService.issueCard(carddto.getCustomerId(), carddto.getAccountId(), carddto.getPin(),carddto.getCardType());

        return CardDTO.fromEntity(card);
    }

    @GetMapping(value = "")
    public List<Card> getCards(@RequestParam(required = false) Long id) {
    	if(id != null)
            return cardService.getCustomerCards(id);
    	else
    		return cardService.getAllCards();
    }
    
    @PatchMapping(value = "/status", produces = "application/json")
    public ResponseEntity<Map<String, Object>> updateCardStatus(
            @RequestParam int id, 
            @RequestParam Card.cardStatus status) {
        
        cardService.updateStatus(id, status);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Status updated to " + status);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping(value = "")
    public ResponseEntity<Map<String, String>> deleteCard(@RequestParam int id) {
        
        cardService.deleteCard(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Card with ID " + id + " has been successfully revoked.");
        
        return ResponseEntity.ok(response);
    }
}