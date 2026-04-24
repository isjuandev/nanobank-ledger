package com.nanobank.ledger.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.auth.repository.UserRepository;
import com.nanobank.ledger.exception.TransactionNotFoundException;
import com.nanobank.ledger.transaction.dto.TransactionFilterDTO;
import com.nanobank.ledger.transaction.dto.TransactionRequestDTO;
import com.nanobank.ledger.transaction.dto.TransactionResponseDTO;
import com.nanobank.ledger.transaction.dto.TransferRequestDTO;
import com.nanobank.ledger.transaction.entity.Transaction;
import com.nanobank.ledger.transaction.entity.TransactionType;
import com.nanobank.ledger.transaction.repository.TransactionRepository;
import com.nanobank.ledger.transaction.service.domain.TransactionBalanceService;
import com.nanobank.ledger.transaction.service.domain.TransactionTransferResolver;
import com.nanobank.ledger.transaction.service.impl.TransactionServiceImpl;
import com.nanobank.ledger.wallet.entity.Wallet;
import com.nanobank.ledger.wallet.entity.WalletType;
import com.nanobank.ledger.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TransactionBalanceService balanceService;

    @Mock
    private TransactionTransferResolver transferResolver;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void createTransaction_income_increasesBalance() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet wallet = buildWallet(10L, "Main", "100", user);
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("50"))
                .type(TransactionType.INGRESO)
                .category("Salary")
                .description("Monthly income")
                .walletId(10L)
                .date(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByIdAndOwner(10L, user)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(99L);
            return tx;
        });

        TransactionResponseDTO response = transactionService.createTransaction(request, "user@nanobank.com");

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("150")));
        assertEquals(99L, response.getId());
    }

    @Test
    void createTransaction_expense_decreasesBalance() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet wallet = buildWallet(10L, "Main", "100", user);
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("30"))
                .type(TransactionType.GASTO)
                .category("Food")
                .description("Lunch")
                .walletId(10L)
                .date(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByIdAndOwner(10L, user)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(request, "user@nanobank.com");

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("70")));
    }

    @Test
    void createTransaction_persistsDescription() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet wallet = buildWallet(10L, "Main", "100", user);
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("30"))
                .type(TransactionType.GASTO)
                .category("Food")
                .description("Lunch with client")
                .walletId(10L)
                .date(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByIdAndOwner(10L, user)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(request, "user@nanobank.com");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals("Lunch with client", captor.getValue().getDescription());
    }

    @Test
    void transferTransaction_updatesSourceAndTargetBalance() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet walletA = buildWallet(1L, "A", "500", user);
        Wallet walletB = buildWallet(2L, "B", "100", user);
        Transaction transaction = buildTransaction(77L, "200", TransactionType.INGRESO, "Salary", walletA);
        TransferRequestDTO request = TransferRequestDTO.builder().targetWalletId(2L).build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(transferResolver.resolve(77L, request, user))
                .thenReturn(new TransactionTransferResolver.TransferContext(transaction, walletA, walletB));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.transferTransaction(77L, request, "user@nanobank.com");

        assertEquals(0, walletA.getBalance().compareTo(new BigDecimal("300")));
        assertEquals(0, walletB.getBalance().compareTo(new BigDecimal("300")));
    }

    @Test
    void transferTransaction_expenseTransfer_correctBalances() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet walletA = buildWallet(1L, "A", "100", user);
        Wallet walletB = buildWallet(2L, "B", "200", user);
        Transaction transaction = buildTransaction(88L, "50", TransactionType.GASTO, "Food", walletA);
        TransferRequestDTO request = TransferRequestDTO.builder().targetWalletId(2L).build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(transferResolver.resolve(88L, request, user))
                .thenReturn(new TransactionTransferResolver.TransferContext(transaction, walletA, walletB));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.transferTransaction(88L, request, "user@nanobank.com");

        assertEquals(0, walletA.getBalance().compareTo(new BigDecimal("150")));
        assertEquals(0, walletB.getBalance().compareTo(new BigDecimal("150")));
    }

    @Test
    void transferTransaction_transactionNotFound_throwsException() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet walletA = buildWallet(1L, "A", "100", user);
        Wallet walletB = buildWallet(2L, "B", "200", user);
        TransferRequestDTO request = TransferRequestDTO.builder().targetWalletId(2L).build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(transferResolver.resolve(999L, request, user))
                .thenThrow(new TransactionNotFoundException("Transacción no encontrada para id: 999"));

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.transferTransaction(999L, request, "user@nanobank.com"));
    }

    @Test
    void getTransactions_withCategoryFilter_returnsFiltered() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet wallet = buildWallet(10L, "Main", "100", user);
        Transaction txMatch = buildTransaction(1L, "20", TransactionType.GASTO, "Food", wallet);
        Transaction txOther = buildTransaction(2L, "10", TransactionType.GASTO, "Transport", wallet);
        TransactionFilterDTO filter = TransactionFilterDTO.builder().category("Food").build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByIdAndOwner(10L, user)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletAndCategoryContainingIgnoreCase(wallet, "Food"))
                .thenReturn(List.of(txMatch, txOther));

        List<TransactionResponseDTO> response = transactionService.getTransactions(10L, filter, "user@nanobank.com");

        assertEquals(1, response.size());
        assertEquals("Food", response.get(0).getCategory());
    }

    @Test
    void getTransactions_withDateRange_returnsFiltered() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet wallet = buildWallet(10L, "Main", "100", user);
        Transaction txInRange = buildTransaction(1L, "20", TransactionType.GASTO, "Food", wallet);
        txInRange.setDate(LocalDateTime.parse("2026-04-15T10:00:00"));
        Transaction txOutRange = buildTransaction(2L, "10", TransactionType.GASTO, "Transport", wallet);
        txOutRange.setDate(LocalDateTime.parse("2026-05-01T10:00:00"));
        TransactionFilterDTO filter = TransactionFilterDTO.builder()
                .dateFrom(LocalDateTime.parse("2026-04-01T00:00:00"))
                .dateTo(LocalDateTime.parse("2026-04-30T23:59:59"))
                .build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByIdAndOwner(10L, user)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletAndDateBetween(
                eq(wallet),
                eq(filter.getDateFrom()),
                eq(filter.getDateTo())
        )).thenReturn(List.of(txInRange, txOutRange));

        List<TransactionResponseDTO> response = transactionService.getTransactions(10L, filter, "user@nanobank.com");

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
    }

    @Test
    void getTransactions_withoutFilters_usesWalletIn() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet wallet = buildWallet(10L, "Main", "100", user);
        Transaction tx = buildTransaction(1L, "20", TransactionType.GASTO, "Food", wallet);
        TransactionFilterDTO filter = TransactionFilterDTO.builder().build();

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByIdAndOwner(10L, user)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIn(List.of(wallet))).thenReturn(List.of(tx));

        List<TransactionResponseDTO> response = transactionService.getTransactions(10L, filter, "user@nanobank.com");

        assertEquals(1, response.size());
        verify(transactionRepository).findByWalletIn(List.of(wallet));
    }

    @Test
    void deleteTransaction_revertsBalanceAndDeletes() {
        User user = buildUser(1L, "user@nanobank.com");
        Wallet wallet = buildWallet(10L, "Main", "100", user);
        Transaction tx = buildTransaction(5L, "25", TransactionType.GASTO, "Food", wallet);

        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByOwner(user)).thenReturn(List.of(wallet));
        when(transactionRepository.findByIdAndWalletIn(5L, List.of(wallet))).thenReturn(Optional.of(tx));

        transactionService.deleteTransaction(5L, "user@nanobank.com");

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("125")));
        verify(transactionRepository).delete(tx);
    }

    private User buildUser(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .name("User")
                .build();
    }

    private Wallet buildWallet(Long id, String name, String balance, User owner) {
        return Wallet.builder()
                .id(id)
                .name(name)
                .type(WalletType.AHORROS)
                .balance(new BigDecimal(balance))
                .owner(owner)
                .build();
    }

    private Transaction buildTransaction(Long id, String amount, TransactionType type, String category, Wallet wallet) {
        return Transaction.builder()
                .id(id)
                .amount(new BigDecimal(amount))
                .type(type)
                .category(category)
                .description("desc")
                .date(LocalDateTime.now())
                .wallet(wallet)
                .build();
    }
}
