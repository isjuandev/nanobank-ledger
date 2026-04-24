package com.nanobank.ledger.transaction.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.nanobank.ledger.transaction.dto.TransactionFilterDTO;
import com.nanobank.ledger.transaction.dto.TransactionRequestDTO;
import com.nanobank.ledger.transaction.dto.TransactionResponseDTO;
import com.nanobank.ledger.transaction.dto.TransferRequestDTO;
import com.nanobank.ledger.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getTransactions(
            @RequestParam Long walletId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TransactionFilterDTO filter = TransactionFilterDTO.builder()
                .category(category)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();
        return ResponseEntity.ok(transactionService.getTransactions(walletId, filter, userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Valid @RequestBody TransactionRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(transactionService.createTransaction(dto, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/transfer")
    public ResponseEntity<TransactionResponseDTO> transferTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info(
                "PATCH /api/transactions/{}/transfer recibido: targetWalletId={}, usuario={}",
                id,
                dto == null ? null : dto.getTargetWalletId(),
                userDetails == null ? null : userDetails.getUsername()
        );
        return ResponseEntity.ok(transactionService.transferTransaction(id, dto, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        transactionService.deleteTransaction(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
