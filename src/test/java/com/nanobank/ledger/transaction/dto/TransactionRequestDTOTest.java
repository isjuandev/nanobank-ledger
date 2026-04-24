package com.nanobank.ledger.transaction.dto;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionRequestDTOTest {

    @Test
    void setDateFromJson_withNull_setsDateNull() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setDate(LocalDateTime.now());

        dto.setDateFromJson(null);

        assertNull(dto.getDate());
    }

    @Test
    void setDateFromJson_withBlank_setsDateNull() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setDate(LocalDateTime.now());

        dto.setDateFromJson("  ");

        assertNull(dto.getDate());
    }

    @Test
    void setDateFromJson_withLocalDateTime_parsesAsIs() {
        TransactionRequestDTO dto = new TransactionRequestDTO();

        dto.setDateFromJson("2026-04-24T14:30:45");

        assertEquals(LocalDateTime.of(2026, 4, 24, 14, 30, 45), dto.getDate());
    }

    @Test
    void setDateFromJson_withDateOnly_setsStartOfDay() {
        TransactionRequestDTO dto = new TransactionRequestDTO();

        dto.setDateFromJson("2026-04-24");

        assertEquals(LocalDateTime.of(2026, 4, 24, 0, 0, 0), dto.getDate());
    }

    @Test
    void setDateFromJson_withOffset_parsesToLocalDateTime() {
        TransactionRequestDTO dto = new TransactionRequestDTO();

        dto.setDateFromJson("2026-04-24T12:30:00Z");

        assertEquals(LocalDateTime.of(2026, 4, 24, 12, 30, 0), dto.getDate());
    }

    @Test
    void setDescriptionFromJson_assignsRawValue() {
        TransactionRequestDTO dto = new TransactionRequestDTO();

        dto.setDescriptionFromJson("  test desc  ");

        assertEquals("  test desc  ", dto.getDescription());
    }
}
