package com.nanobank.ledger.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import com.nanobank.ledger.transaction.entity.TransactionType;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDTO {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    @NotBlank
    private String category;

    private String description;

    @NotNull
    private Long walletId;

    private LocalDateTime date;

    @JsonSetter("date")
    public void setDateFromJson(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            this.date = null;
            return;
        }

        this.date = parseDate(rawDate);
    }

    @JsonSetter("description")
    public void setDescriptionFromJson(String rawDescription) {
        this.description = rawDescription;
    }

    private LocalDateTime parseDate(String rawDate) {
        try {
            // yyyy-MM-ddTHH:mm[:ss[.SSS]]
            return LocalDateTime.parse(rawDate);
        } catch (DateTimeParseException ignored) {
            try {
                // yyyy-MM-dd (date-only from HTML input[type=date])
                return LocalDate.parse(rawDate).atStartOfDay();
            } catch (DateTimeParseException ignoredAgain) {
                // ISO with timezone/offset (e.g. 2026-04-24T12:30:00Z / -05:00)
                return OffsetDateTime.parse(rawDate).toLocalDateTime();
            }
        }
    }
}
