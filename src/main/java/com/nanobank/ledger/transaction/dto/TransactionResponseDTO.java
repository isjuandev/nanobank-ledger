package com.nanobank.ledger.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nanobank.ledger.transaction.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponseDTO {
    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private String category;
    private String description;
    private LocalDateTime date;
    private Long walletId;
    private String walletName;
}
