package com.nanobank.ledger.transaction.mapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.nanobank.ledger.transaction.dto.TransactionRequestDTO;
import com.nanobank.ledger.transaction.dto.TransactionResponseDTO;
import com.nanobank.ledger.transaction.entity.Transaction;
import com.nanobank.ledger.wallet.entity.Wallet;

public final class TransactionMapper {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    private TransactionMapper() {
    }

    public static Transaction toEntity(TransactionRequestDTO dto, Wallet wallet) {
        return Transaction.builder()
                .amount(dto.getAmount())
                .type(dto.getType())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .date(dto.getDate() == null ? LocalDateTime.now(BOGOTA_ZONE) : dto.getDate())
                .wallet(wallet)
                .build();
    }

    public static TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .date(transaction.getDate())
                .walletId(transaction.getWallet().getId())
                .walletName(transaction.getWallet().getName())
                .build();
    }
}
