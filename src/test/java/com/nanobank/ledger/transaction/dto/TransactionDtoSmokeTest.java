package com.nanobank.ledger.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nanobank.ledger.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionDtoSmokeTest {

    @Test
    void transactionFilterDto_builderAndGetters_work() {
        LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);

        TransactionFilterDTO dto = TransactionFilterDTO.builder()
                .category("food")
                .dateFrom(from)
                .dateTo(to)
                .build();

        assertEquals("food", dto.getCategory());
        assertEquals(from, dto.getDateFrom());
        assertEquals(to, dto.getDateTo());
    }

    @Test
    void transactionResponseDto_builderAndGetters_work() {
        LocalDateTime date = LocalDateTime.of(2026, 4, 24, 15, 10);

        TransactionResponseDTO dto = TransactionResponseDTO.builder()
                .id(77L)
                .amount(new BigDecimal("123.45"))
                .type(TransactionType.INGRESO)
                .category("salary")
                .description("April payroll")
                .date(date)
                .walletId(10L)
                .walletName("Main")
                .build();

        assertEquals(77L, dto.getId());
        assertEquals(new BigDecimal("123.45"), dto.getAmount());
        assertEquals(TransactionType.INGRESO, dto.getType());
        assertEquals("salary", dto.getCategory());
        assertEquals("April payroll", dto.getDescription());
        assertEquals(date, dto.getDate());
        assertEquals(10L, dto.getWalletId());
        assertEquals("Main", dto.getWalletName());
    }

    @Test
    void transferRequestDto_builderAndSetter_work() {
        TransferRequestDTO dto = TransferRequestDTO.builder()
                .targetWalletId(20L)
                .build();

        assertEquals(20L, dto.getTargetWalletId());

        dto.setTargetWalletId(30L);
        assertEquals(30L, dto.getTargetWalletId());
    }
}
