package com.nanobank.ledger.transaction.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nanobank.ledger.transaction.dto.TransactionRequestDTO;
import com.nanobank.ledger.transaction.dto.TransactionResponseDTO;
import com.nanobank.ledger.transaction.entity.Transaction;
import com.nanobank.ledger.transaction.entity.TransactionType;
import com.nanobank.ledger.wallet.entity.Wallet;
import com.nanobank.ledger.wallet.entity.WalletType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionMapperTest {

    @Test
    void toEntity_withNullDate_setsCurrentDate() {
        TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .amount(new BigDecimal("35.00"))
                .type(TransactionType.GASTO)
                .category("COMIDA")
                .description("almuerzo")
                .walletId(10L)
                .date(null)
                .build();

        Wallet wallet = Wallet.builder()
                .id(10L)
                .name("Main")
                .type(WalletType.AHORROS)
                .build();

        Transaction entity = TransactionMapper.toEntity(dto, wallet);

        assertNotNull(entity.getDate());
        assertEquals(wallet, entity.getWallet());
        assertEquals(new BigDecimal("35.00"), entity.getAmount());
    }

    @Test
    void toResponseDto_mapsWalletData() {
        Wallet wallet = Wallet.builder()
                .id(10L)
                .name("Main")
                .type(WalletType.AHORROS)
                .build();

        Transaction transaction = Transaction.builder()
                .id(11L)
                .amount(new BigDecimal("99.99"))
                .type(TransactionType.INGRESO)
                .category("SALARIO")
                .description("abril")
                .date(LocalDateTime.of(2026, 4, 24, 12, 0))
                .wallet(wallet)
                .build();

        TransactionResponseDTO response = TransactionMapper.toResponseDTO(transaction);

        assertEquals(10L, response.getWalletId());
        assertEquals("Main", response.getWalletName());
        assertEquals(11L, response.getId());
    }
}
