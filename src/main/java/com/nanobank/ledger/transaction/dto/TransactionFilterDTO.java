package com.nanobank.ledger.transaction.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionFilterDTO {
    private String category;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
