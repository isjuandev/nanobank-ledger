package com.nanobank.ledger.wallet.mapper;

import java.math.BigDecimal;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.wallet.dto.WalletRequestDTO;
import com.nanobank.ledger.wallet.dto.WalletResponseDTO;
import com.nanobank.ledger.wallet.entity.Wallet;

public final class WalletMapper {

    private WalletMapper() {
    }

    public static Wallet toEntity(WalletRequestDTO dto, User owner) {
        return Wallet.builder()
                .name(dto.getName())
                .type(dto.getType())
                .balance(BigDecimal.ZERO)
                .owner(owner)
                .build();
    }

    public static WalletResponseDTO toResponseDTO(Wallet wallet, long transactionCount) {
        return WalletResponseDTO.builder()
                .id(wallet.getId())
                .name(wallet.getName())
                .type(wallet.getType())
                .balance(wallet.getBalance())
                .transactionCount(transactionCount)
                .build();
    }
}
