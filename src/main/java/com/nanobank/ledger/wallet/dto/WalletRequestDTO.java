package com.nanobank.ledger.wallet.dto;

import com.nanobank.ledger.wallet.entity.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequestDTO {

    @NotBlank
    private String name;

    @NotNull
    private WalletType type;
}
