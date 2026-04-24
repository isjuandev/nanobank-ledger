package com.nanobank.ledger.wallet.dto;

import java.math.BigDecimal;

import com.nanobank.ledger.wallet.entity.WalletType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletResponseDTO {
    private Long id;
    private String name;
    private WalletType type;
    private BigDecimal balance;
    private long transactionCount;
}
