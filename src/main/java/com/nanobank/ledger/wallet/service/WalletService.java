package com.nanobank.ledger.wallet.service;

import java.util.List;

import com.nanobank.ledger.wallet.dto.WalletRequestDTO;
import com.nanobank.ledger.wallet.dto.WalletResponseDTO;

public interface WalletService {
    WalletResponseDTO createWallet(WalletRequestDTO dto, String ownerEmail);
    List<WalletResponseDTO> getWallets(String ownerEmail);
    WalletResponseDTO getWalletById(Long id, String ownerEmail);
    void deleteWallet(Long id, String ownerEmail);
}
