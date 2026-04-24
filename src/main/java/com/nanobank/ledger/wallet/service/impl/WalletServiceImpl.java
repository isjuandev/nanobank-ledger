package com.nanobank.ledger.wallet.service.impl;

import java.util.List;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.auth.repository.UserRepository;
import com.nanobank.ledger.exception.UnauthorizedResourceException;
import com.nanobank.ledger.exception.WalletDeletionNotAllowedException;
import com.nanobank.ledger.exception.WalletNotFoundException;
import com.nanobank.ledger.transaction.repository.TransactionRepository;
import com.nanobank.ledger.wallet.dto.WalletRequestDTO;
import com.nanobank.ledger.wallet.dto.WalletResponseDTO;
import com.nanobank.ledger.wallet.entity.Wallet;
import com.nanobank.ledger.wallet.mapper.WalletMapper;
import com.nanobank.ledger.wallet.repository.WalletRepository;
import com.nanobank.ledger.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public WalletResponseDTO createWallet(WalletRequestDTO dto, String ownerEmail) {
        User owner = findOwnerByEmail(ownerEmail);
        Wallet wallet = WalletMapper.toEntity(dto, owner);
        Wallet savedWallet = walletRepository.save(wallet);
        return WalletMapper.toResponseDTO(savedWallet, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponseDTO> getWallets(String ownerEmail) {
        User owner = findOwnerByEmail(ownerEmail);
        return walletRepository.findByOwner(owner)
                .stream()
                .map(wallet -> WalletMapper.toResponseDTO(wallet, transactionRepository.countByWallet(wallet)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDTO getWalletById(Long id, String ownerEmail) {
        User owner = findOwnerByEmail(ownerEmail);
        Wallet wallet = walletRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new WalletNotFoundException("Billetera no encontrada para id: " + id));
        long transactionCount = transactionRepository.countByWallet(wallet);
        return WalletMapper.toResponseDTO(wallet, transactionCount);
    }

    @Override
    @Transactional
    public void deleteWallet(Long id, String ownerEmail) {
        User owner = findOwnerByEmail(ownerEmail);
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException("Billetera no encontrada para id: " + id));

        if (!wallet.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedResourceException("La billetera no pertenece al usuario autenticado");
        }

        long transactionCount = transactionRepository.countByWallet(wallet);
        if (transactionCount > 0) {
            throw new WalletDeletionNotAllowedException("No puedes eliminar una billetera con transacciones.");
        }
        walletRepository.delete(wallet);
    }

    private User findOwnerByEmail(String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new WalletNotFoundException("Propietario no encontrado para correo: " + ownerEmail));
    }
}
