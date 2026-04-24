package com.nanobank.ledger.transaction.service.impl;

import java.math.BigDecimal;
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
import com.nanobank.ledger.transaction.entity.TransactionType;
import com.nanobank.ledger.transaction.mapper.TransactionMapper;
import com.nanobank.ledger.transaction.repository.TransactionRepository;
import com.nanobank.ledger.transaction.service.TransactionService;
import com.nanobank.ledger.wallet.entity.Wallet;
import com.nanobank.ledger.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TransactionResponseDTO createTransaction(TransactionRequestDTO dto, String userEmail) {
        User user = findUserByEmail(userEmail);
        Wallet wallet = findWalletByIdAndOwner(dto.getWalletId(), user);

        Transaction transaction = TransactionMapper.toEntity(dto, wallet);
        applyTransactionEffect(wallet, transaction.getType(), transaction.getAmount());
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
        log.info(
                "Iniciando transferencia transactionId={}, targetWalletId={}, userEmail={}",
                transactionId,
                dto == null ? null : dto.getTargetWalletId(),
                userEmail
        );
        User user = findUserByEmail(userEmail);
        log.info("Usuario de transferencia encontrado: userId={}, email={}", user.getId(), user.getEmail());

        List<Wallet> userWallets = walletRepository.findByOwner(user);
        log.info(
                "Billeteras encontradas para usuario {}: count={}, walletIds={}",
                userEmail,
                userWallets.size(),
                userWallets.stream().map(Wallet::getId).toList()
        );
        if (userWallets.isEmpty()) {
            throw new WalletNotFoundException("No se encontraron billeteras para el usuario autenticado");
        }

        Transaction transaction = transactionRepository.findByIdAndWalletIn(transactionId, userWallets)
                .orElseThrow(() -> new TransactionNotFoundException("Transacción no encontrada para id: " + transactionId));
        log.info(
                "Transacción de transferencia encontrada: id={}, sourceWalletId={}, type={}, amount={}",
                transaction.getId(),
                transaction.getWallet() == null ? null : transaction.getWallet().getId(),
                transaction.getType(),
                transaction.getAmount()
        );
        if (transaction.getWallet() == null) {
            throw new TransactionNotFoundException("Falta la referencia de billetera en la transacción con id: " + transactionId);
        }

        Wallet sourceWallet = transaction.getWallet();
        Wallet targetWallet = userWallets.stream()
                .filter(wallet -> wallet.getId().equals(dto.getTargetWalletId()))
                .findFirst()
                .orElseThrow(() -> new WalletNotFoundException(
                        "Billetera destino no encontrada para id: " + dto.getTargetWalletId()));

        ensureWalletBalanceInitialized(sourceWallet);
        ensureWalletBalanceInitialized(targetWallet);
        log.info(
                "Billeteras de transferencia resueltas: sourceWalletId={}, sourceBalance={}, targetWalletId={}, targetBalance={}",
                sourceWallet.getId(),
                sourceWallet.getBalance(),
                targetWallet.getId(),
                targetWallet.getBalance()
        );

        // No-op transfer: same source and target wallet.
        if (sourceWallet.getId().equals(targetWallet.getId())) {
            log.info("Transferencia omitida porque la billetera origen y destino son la misma: walletId={}", sourceWallet.getId());
            return TransactionMapper.toResponseDTO(transaction);
        }

        BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
        if (transaction.getType() == null) {
            throw new TransactionNotFoundException("Falta el tipo de transacción para id: " + transactionId);
        }
        revertTransactionEffect(sourceWallet, transaction.getType(), amount);
        applyTransactionEffect(targetWallet, transaction.getType(), amount);
        log.info(
                "Balances después de recalcular transferencia: sourceWalletId={}, sourceBalance={}, targetWalletId={}, targetBalance={}",
                sourceWallet.getId(),
                sourceWallet.getBalance(),
                targetWallet.getId(),
                targetWallet.getBalance()
        );

        transaction.setWallet(targetWallet);

        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);
        Transaction updatedTransaction = transactionRepository.save(transaction);
        log.info(
                "Transferencia guardada correctamente: transactionId={}, newWalletId={}",
                updatedTransaction.getId(),
                updatedTransaction.getWallet() == null ? null : updatedTransaction.getWallet().getId()
        );
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
        revertTransactionEffect(wallet, transaction.getType(), transaction.getAmount());
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

    private void applyTransactionEffect(Wallet wallet, TransactionType type, BigDecimal amount) {
        ensureWalletBalanceInitialized(wallet);
        if (type == TransactionType.INGRESO) {
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        }
    }

    private void revertTransactionEffect(Wallet wallet, TransactionType type, BigDecimal amount) {
        ensureWalletBalanceInitialized(wallet);
        if (type == TransactionType.INGRESO) {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        } else {
            wallet.setBalance(wallet.getBalance().add(amount));
        }
    }

    private void ensureWalletBalanceInitialized(Wallet wallet) {
        if (wallet.getBalance() == null) {
            wallet.setBalance(BigDecimal.ZERO);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
