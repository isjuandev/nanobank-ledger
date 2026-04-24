package com.nanobank.ledger.wallet.repository;

import java.util.List;
import java.util.Optional;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByOwner(User owner);
    Optional<Wallet> findByIdAndOwner(Long id, User owner);
}
