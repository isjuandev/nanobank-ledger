package com.nanobank.ledger.transaction.service.domain;

import java.math.BigDecimal;

import com.nanobank.ledger.transaction.entity.TransactionType;
import com.nanobank.ledger.wallet.entity.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TransactionBalanceServiceTest {

    private final TransactionBalanceService service = new TransactionBalanceService();

    @Test
    void applyEffect_whenTypeIsIngreso_addsAmountToBalance() {
        Wallet wallet = Wallet.builder().balance(new BigDecimal("100")).build();

        service.applyEffect(wallet, TransactionType.INGRESO, new BigDecimal("25"));

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("125")));
    }

    @Test
    void applyEffect_whenTypeIsGasto_subtractsAmountFromBalance() {
        Wallet wallet = Wallet.builder().balance(new BigDecimal("100")).build();

        service.applyEffect(wallet, TransactionType.GASTO, new BigDecimal("25"));

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("75")));
    }

    @Test
    void revertEffect_whenTypeIsIngreso_subtractsAmountFromBalance() {
        Wallet wallet = Wallet.builder().balance(new BigDecimal("100")).build();

        service.revertEffect(wallet, TransactionType.INGRESO, new BigDecimal("25"));

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("75")));
    }

    @Test
    void revertEffect_whenTypeIsGasto_addsAmountToBalance() {
        Wallet wallet = Wallet.builder().balance(new BigDecimal("100")).build();

        service.revertEffect(wallet, TransactionType.GASTO, new BigDecimal("25"));

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("125")));
    }

    @Test
    void ensureWalletBalanceInitialized_whenBalanceIsNull_setsZero() {
        Wallet wallet = Wallet.builder().balance(null).build();

        service.ensureWalletBalanceInitialized(wallet);

        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void ensureWalletBalanceInitialized_whenBalanceExists_keepsValue() {
        Wallet wallet = Wallet.builder().balance(new BigDecimal("12.50")).build();

        service.ensureWalletBalanceInitialized(wallet);

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("12.50")));
    }
}
