package com.nanobank.ledger.transaction.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.auth.repository.UserRepository;
import com.nanobank.ledger.exception.TransactionNotFoundException;
import com.nanobank.ledger.exception.WalletNotFoundException;
import com.nanobank.ledger.transaction.dto.TransactionFilterDTO;
import com.nanobank.ledger.transaction.dto.TransactionRequestDTO;
import com.nanobank.ledger.transaction.dto.TransactionResponseDTO;
import com.nanobank.ledger.transaction.dto.TransferRequestDTO;
import com.nanobank.ledger.transaction.entity.Transaction;
import com.nanobank.ledger.transaction.mapper.TransactionMapper;
import com.nanobank.ledger.transaction.repository.TransactionRepository;
import com.nanobank.ledger.transaction.service.TransactionService;
import com.nanobank.ledger.transaction.service.domain.TransactionBalanceService;
import com.nanobank.ledger.transaction.service.domain.TransactionTransferResolver;
import com.nanobank.ledger.wallet.entity.Wallet;
import com.nanobank.ledger.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionBalanceService balanceService;
    private final TransactionTransferResolver transferResolver;

    @Override
    @Transactional
    public TransactionResponseDTO createTransaction(TransactionRequestDTO dto, String userEmail) {
        User user = findUserByEmail(userEmail);
        Wallet wallet = findWalletByIdAndOwner(dto.getWalletId(), user);

        Transaction transaction = TransactionMapper.toEntity(dto, wallet);
        balanceService.applyEffect(wallet, transaction.getType(), transaction.getAmount());
        walletRepository.save(wallet);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransactionMapper.toResponseDTO(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactions(Long walletId, TransactionFilterDTO filter, String userEmail) {
        User user = findUserByEmail(userEmail);
        Wallet wallet = findWalletByIdAndOwner(walletId, user);

        String category = filter == null ? null : filter.getCategory();
        LocalDateTime dateFrom = filter == null ? null : filter.getDateFrom();
        LocalDateTime dateTo = filter == null ? null : filter.getDateTo();

        List<Transaction> transactions;
        if (hasText(category)) {
            transactions = transactionRepository.findByWalletAndCategoryContainingIgnoreCase(wallet, category);
        } else if (dateFrom != null && dateTo != null) {
            transactions = transactionRepository.findByWalletAndDateBetween(wallet, dateFrom, dateTo);
        } else {
            transactions = transactionRepository.findByWalletIn(List.of(wallet));
        }

        return transactions.stream()
                .filter(t -> !hasText(category) || t.getCategory().toLowerCase().contains(category.toLowerCase()))
                .filter(t -> dateFrom == null || !t.getDate().isBefore(dateFrom))
                .filter(t -> dateTo == null || !t.getDate().isAfter(dateTo))
                .map(TransactionMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public TransactionResponseDTO transferTransaction(Long transactionId, TransferRequestDTO dto, String userEmail) {
        User user = findUserByEmail(userEmail);
        TransactionTransferResolver.TransferContext context = transferResolver.resolve(transactionId, dto, user);
        Transaction transaction = context.transaction();
        Wallet sourceWallet = context.sourceWallet();
        Wallet targetWallet = context.targetWallet();

        balanceService.ensureWalletBalanceInitialized(sourceWallet);
        balanceService.ensureWalletBalanceInitialized(targetWallet);

        // No-op transfer: same source and target wallet.
        if (sourceWallet.getId().equals(targetWallet.getId())) {
            return TransactionMapper.toResponseDTO(transaction);
        }

        if (transaction.getType() == null) {
            throw new TransactionNotFoundException("Falta el tipo de transacción para id: " + transactionId);
        }
        balanceService.revertEffect(sourceWallet, transaction.getType(), transaction.getAmount());
        balanceService.applyEffect(targetWallet, transaction.getType(), transaction.getAmount());

        transaction.setWallet(targetWallet);

        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);
        Transaction updatedTransaction = transactionRepository.save(transaction);
        return TransactionMapper.toResponseDTO(updatedTransaction);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id, String userEmail) {
        User user = findUserByEmail(userEmail);
        List<Wallet> userWallets = walletRepository.findByOwner(user);
        if (userWallets.isEmpty()) {
            throw new WalletNotFoundException("No se encontraron billeteras para el usuario autenticado");
        }
        Transaction transaction = transactionRepository.findByIdAndWalletIn(id, userWallets)
                .orElseThrow(() -> new TransactionNotFoundException("Transacción no encontrada para id: " + id));

        Wallet wallet = transaction.getWallet();
        balanceService.revertEffect(wallet, transaction.getType(), transaction.getAmount());
        walletRepository.save(wallet);

        transactionRepository.delete(transaction);
    }

    private User findUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new WalletNotFoundException("Usuario no encontrado para correo: " + userEmail));
    }

    private Wallet findWalletByIdAndOwner(Long walletId, User owner) {
        return walletRepository.findByIdAndOwner(walletId, owner)
                .orElseThrow(() -> new WalletNotFoundException("Billetera no encontrada para id: " + walletId));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
