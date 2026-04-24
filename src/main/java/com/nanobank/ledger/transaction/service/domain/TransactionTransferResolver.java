package com.nanobank.ledger.transaction.service.domain;

import java.util.List;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.exception.TransactionNotFoundException;
import com.nanobank.ledger.exception.WalletNotFoundException;
import com.nanobank.ledger.transaction.dto.TransferRequestDTO;
import com.nanobank.ledger.transaction.entity.Transaction;
import com.nanobank.ledger.transaction.repository.TransactionRepository;
import com.nanobank.ledger.wallet.entity.Wallet;
import com.nanobank.ledger.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionTransferResolver {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransferContext resolve(Long transactionId, TransferRequestDTO dto, User user) {
        List<Wallet> userWallets = walletRepository.findByOwner(user);
        if (userWallets.isEmpty()) {
            throw new WalletNotFoundException("No se encontraron billeteras para el usuario autenticado");
        }

        Transaction transaction = transactionRepository.findByIdAndWalletIn(transactionId, userWallets)
                .orElseThrow(() -> new TransactionNotFoundException("Transacción no encontrada para id: " + transactionId));

        if (transaction.getWallet() == null) {
            throw new TransactionNotFoundException("Falta la referencia de billetera en la transacción con id: " + transactionId);
        }

        Wallet sourceWallet = transaction.getWallet();
        Wallet targetWallet = userWallets.stream()
                .filter(wallet -> wallet.getId().equals(dto.getTargetWalletId()))
                .findFirst()
                .orElseThrow(() -> new WalletNotFoundException(
                        "Billetera destino no encontrada para id: " + dto.getTargetWalletId()));

        return new TransferContext(transaction, sourceWallet, targetWallet);
    }

    public record TransferContext(Transaction transaction, Wallet sourceWallet, Wallet targetWallet) {
    }
}
