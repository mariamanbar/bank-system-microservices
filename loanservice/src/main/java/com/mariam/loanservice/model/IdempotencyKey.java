package com.mariam.loanservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    private String key; // "DEBIT_20231125103000_101_500"

    private LocalDateTime createdAt;
}