package com.example.walletservice.dto;

import com.example.walletservice.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    
    private Long id;
    private Long walletId;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private LocalDateTime timestamp;
    private BigDecimal balanceAfterTransaction;
    private Long relatedWalletId;
}