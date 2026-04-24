package com.nanobank.ledger.transaction.service.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.exception.TransactionNotFoundException;
import com.nanobank.ledger.exception.WalletNotFoundException;
import com.nanobank.ledger.transaction.dto.TransferRequestDTO;
import com.nanobank.ledger.transaction.entity.Transaction;
import com.nanobank.ledger.transaction.entity.TransactionType;
import com.nanobank.ledger.transaction.repository.TransactionRepository;
import com.nanobank.ledger.wallet.entity.Wallet;
import com.nanobank.ledger.wallet.entity.WalletType;
import com.nanobank.ledger.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionTransferResolverTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransactionTransferResolver resolver;

    @Test
    void resolve_whenUserWalletsEmpty_throwsWalletNotFoundException() {
        User user = buildUser(1L, "user@nanobank.com");

        when(walletRepository.findByOwner(user)).thenReturn(List.of());

        assertThrows(WalletNotFoundException.class, () -> resolver.resolve(10L, dto(20L), user));
    }

    @Test
    void resolve_whenUserWalletsNotEmpty_continuesAndReturnsContext() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet source = buildWallet(10L, user);
        Wallet target = buildWallet(20L, user);
        Transaction transaction = buildTransaction(99L, source);

        when(walletRepository.findByOwner(user)).thenReturn(List.of(source, target));
        when(transactionRepository.findByIdAndWalletIn(99L, List.of(source, target)))
                .thenReturn(Optional.of(transaction));

        TransactionTransferResolver.TransferContext context = resolver.resolve(99L, dto(20L), user);

        assertEquals(transaction, context.transaction());
        assertEquals(source, context.sourceWallet());
        assertEquals(target, context.targetWallet());
    }

    @Test
    void resolve_whenTransactionWalletIsNull_throwsTransactionNotFoundException() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet target = buildWallet(20L, user);
        Transaction transaction = buildTransaction(99L, null);

        when(walletRepository.findByOwner(user)).thenReturn(List.of(target));
        when(transactionRepository.findByIdAndWalletIn(99L, List.of(target)))
                .thenReturn(Optional.of(transaction));

        assertThrows(TransactionNotFoundException.class, () -> resolver.resolve(99L, dto(20L), user));
    }

    @Test
    void resolve_whenTransactionWalletIsNotNull_returnsContext() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet source = buildWallet(10L, user);
        Wallet target = buildWallet(20L, user);
        Transaction transaction = buildTransaction(99L, source);

        when(walletRepository.findByOwner(user)).thenReturn(List.of(source, target));
        when(transactionRepository.findByIdAndWalletIn(99L, List.of(source, target)))
                .thenReturn(Optional.of(transaction));

        TransactionTransferResolver.TransferContext context = resolver.resolve(99L, dto(20L), user);

        assertEquals(source, context.sourceWallet());
        assertEquals(target, context.targetWallet());
    }

    private static TransferRequestDTO dto(Long targetWalletId) {
        return TransferRequestDTO.builder().targetWalletId(targetWalletId).build();
    }

    private static User buildUser(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .name("User")
                .build();
    }

    private static Wallet buildWallet(Long id, User owner) {
        return Wallet.builder()
                .id(id)
                .name("Wallet " + id)
                .type(WalletType.AHORROS)
                .balance(new BigDecimal("100"))
                .owner(owner)
                .build();
    }

    private static Transaction buildTransaction(Long id, Wallet wallet) {
        return Transaction.builder()
                .id(id)
                .amount(new BigDecimal("10"))
                .type(TransactionType.GASTO)
                .category("Food")
                .description("Test")
                .date(LocalDateTime.parse("2026-04-24T10:00:00"))
                .wallet(wallet)
                .build();
    }
}
