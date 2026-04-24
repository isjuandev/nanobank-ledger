package com.nanobank.ledger.transaction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.nanobank.ledger.transaction.entity.Transaction;
import com.nanobank.ledger.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    long countByWallet(Wallet wallet);
    List<Transaction> findByWalletAndCategoryContainingIgnoreCase(Wallet wallet, String category);
    List<Transaction> findByWalletAndDateBetween(Wallet wallet, LocalDateTime from, LocalDateTime to);
    List<Transaction> findByWalletIn(List<Wallet> wallets);
    Optional<Transaction> findByIdAndWalletIn(Long id, List<Wallet> wallets);
    void deleteByWallet(Wallet wallet);
}
