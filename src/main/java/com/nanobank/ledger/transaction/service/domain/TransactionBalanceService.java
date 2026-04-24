package com.nanobank.ledger.transaction.service.domain;

import java.math.BigDecimal;

import com.nanobank.ledger.transaction.entity.TransactionType;
import com.nanobank.ledger.wallet.entity.Wallet;
import org.springframework.stereotype.Service;

@Service
public class TransactionBalanceService {

    public void applyEffect(Wallet wallet, TransactionType type, BigDecimal amount) {
        ensureWalletBalanceInitialized(wallet);
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        if (type == TransactionType.INGRESO) {
            wallet.setBalance(wallet.getBalance().add(safeAmount));
            return;
        }
        wallet.setBalance(wallet.getBalance().subtract(safeAmount));
    }

    public void revertEffect(Wallet wallet, TransactionType type, BigDecimal amount) {
        ensureWalletBalanceInitialized(wallet);
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        if (type == TransactionType.INGRESO) {
            wallet.setBalance(wallet.getBalance().subtract(safeAmount));
            return;
        }
        wallet.setBalance(wallet.getBalance().add(safeAmount));
    }

    public void ensureWalletBalanceInitialized(Wallet wallet) {
        if (wallet.getBalance() == null) {
            wallet.setBalance(BigDecimal.ZERO);
        }
    }
}
