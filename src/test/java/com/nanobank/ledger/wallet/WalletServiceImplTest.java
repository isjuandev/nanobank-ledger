package com.nanobank.ledger.wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.auth.repository.UserRepository;
import com.nanobank.ledger.exception.UnauthorizedResourceException;
import com.nanobank.ledger.exception.WalletDeletionNotAllowedException;
import com.nanobank.ledger.exception.WalletNotFoundException;
import com.nanobank.ledger.transaction.repository.TransactionRepository;
import com.nanobank.ledger.wallet.dto.WalletRequestDTO;
import com.nanobank.ledger.wallet.dto.WalletResponseDTO;
import com.nanobank.ledger.wallet.entity.Wallet;
import com.nanobank.ledger.wallet.entity.WalletType;
import com.nanobank.ledger.wallet.repository.WalletRepository;
import com.nanobank.ledger.wallet.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void createWallet_success() {
        User owner = buildUser(1L, "owner@nanobank.com");
        WalletRequestDTO request = WalletRequestDTO.builder()
                .name("Ahorros")
                .type(WalletType.AHORROS)
                .build();

        when(userRepository.findByEmail("owner@nanobank.com")).thenReturn(Optional.of(owner));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(10L);
            return wallet;
        });

        WalletResponseDTO response = walletService.createWallet(request, "owner@nanobank.com");

        assertEquals(10L, response.getId());
        assertEquals("Ahorros", response.getName());
        assertEquals(WalletType.AHORROS, response.getType());
        assertEquals(0, response.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0L, response.getTransactionCount());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_userNotFound() {
        WalletRequestDTO request = WalletRequestDTO.builder()
                .name("Gastos")
                .type(WalletType.GASTOS)
                .build();
        when(userRepository.findByEmail("missing@nanobank.com")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.createWallet(request, "missing@nanobank.com"));
    }

    @Test
    void getWallets_returnsOnlyOwnerWallets() {
        User owner = buildUser(1L, "owner@nanobank.com");
        Wallet walletOne = buildWallet(11L, "Ahorros", WalletType.AHORROS, "100", owner);
        Wallet walletTwo = buildWallet(12L, "Gastos", WalletType.GASTOS, "50", owner);

        when(userRepository.findByEmail("owner@nanobank.com")).thenReturn(Optional.of(owner));
        when(walletRepository.findByOwner(owner)).thenReturn(List.of(walletOne, walletTwo));
        when(transactionRepository.countByWallet(walletOne)).thenReturn(3L);
        when(transactionRepository.countByWallet(walletTwo)).thenReturn(1L);

        List<WalletResponseDTO> response = walletService.getWallets("owner@nanobank.com");

        assertEquals(2, response.size());
        assertTrue(response.stream().allMatch(r -> r.getId().equals(11L) || r.getId().equals(12L)));
        verify(walletRepository).findByOwner(owner);
    }

    @Test
    void deleteWallet_notOwner() {
        User authenticatedUser = buildUser(1L, "owner@nanobank.com");
        User otherUser = buildUser(2L, "other@nanobank.com");
        Wallet wallet = buildWallet(55L, "Privada", WalletType.INVERSION, "500", otherUser);

        when(userRepository.findByEmail("owner@nanobank.com")).thenReturn(Optional.of(authenticatedUser));
        when(walletRepository.findById(55L)).thenReturn(Optional.of(wallet));

        assertThrows(UnauthorizedResourceException.class,
                () -> walletService.deleteWallet(55L, "owner@nanobank.com"));
        verify(walletRepository, never()).delete(any(Wallet.class));
    }

    @Test
    void getWalletById_success() {
        User owner = buildUser(1L, "owner@nanobank.com");
        Wallet wallet = buildWallet(33L, "Ahorros", WalletType.AHORROS, "120", owner);

        when(userRepository.findByEmail("owner@nanobank.com")).thenReturn(Optional.of(owner));
        when(walletRepository.findByIdAndOwner(33L, owner)).thenReturn(Optional.of(wallet));
        when(transactionRepository.countByWallet(wallet)).thenReturn(4L);

        WalletResponseDTO response = walletService.getWalletById(33L, "owner@nanobank.com");

        assertEquals(33L, response.getId());
        assertEquals(4L, response.getTransactionCount());
    }

    @Test
    void deleteWallet_success() {
        User owner = buildUser(1L, "owner@nanobank.com");
        Wallet wallet = buildWallet(40L, "Main", WalletType.AHORROS, "100", owner);

        when(userRepository.findByEmail("owner@nanobank.com")).thenReturn(Optional.of(owner));
        when(walletRepository.findById(40L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.countByWallet(wallet)).thenReturn(0L);

        walletService.deleteWallet(40L, "owner@nanobank.com");

        verify(transactionRepository).countByWallet(wallet);
        verify(walletRepository).delete(wallet);
    }

    @Test
    void deleteWallet_withTransactions_throwsWalletDeletionNotAllowedException() {
        User owner = buildUser(1L, "owner@nanobank.com");
        Wallet wallet = buildWallet(40L, "Main", WalletType.AHORROS, "100", owner);

        when(userRepository.findByEmail("owner@nanobank.com")).thenReturn(Optional.of(owner));
        when(walletRepository.findById(40L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.countByWallet(wallet)).thenReturn(2L);

        assertThrows(WalletDeletionNotAllowedException.class,
                () -> walletService.deleteWallet(40L, "owner@nanobank.com"));
        verify(walletRepository, never()).delete(wallet);
    }

    private User buildUser(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .name("User")
                .build();
    }

    private Wallet buildWallet(Long id, String name, WalletType type, String balance, User owner) {
        return Wallet.builder()
                .id(id)
                .name(name)
                .type(type)
                .balance(new BigDecimal(balance))
                .owner(owner)
                .build();
    }
}
