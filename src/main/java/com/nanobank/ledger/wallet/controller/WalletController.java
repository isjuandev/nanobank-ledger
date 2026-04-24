package com.nanobank.ledger.wallet.controller;

import java.util.List;

import com.nanobank.ledger.exception.UnauthorizedResourceException;
import com.nanobank.ledger.wallet.dto.WalletRequestDTO;
import com.nanobank.ledger.wallet.dto.WalletResponseDTO;
import com.nanobank.ledger.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<List<WalletResponseDTO>> getWallets(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.getWallets(requireAuthenticatedEmail(userDetails)));
    }

    @PostMapping
    public ResponseEntity<WalletResponseDTO> createWallet(
            @Valid @RequestBody WalletRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(walletService.createWallet(dto, requireAuthenticatedEmail(userDetails)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponseDTO> getWalletById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(walletService.getWalletById(id, requireAuthenticatedEmail(userDetails)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        walletService.deleteWallet(id, requireAuthenticatedEmail(userDetails));
        return ResponseEntity.noContent().build();
    }

    private String requireAuthenticatedEmail(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new UnauthorizedResourceException("Usuario no autenticado");
        }
        return userDetails.getUsername();
    }
}
