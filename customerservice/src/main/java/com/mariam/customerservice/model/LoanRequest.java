package com.mariam.customerservice.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class LoanRequest {
    private int customerId;
    private float amount;
}