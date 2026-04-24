package com.nanobank.ledger.wallet.controller;

import com.nanobank.ledger.exception.UnauthorizedResourceException;
import com.nanobank.ledger.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WalletControllerUnitTest {

    private final WalletService walletService = mock(WalletService.class);
    private final WalletController walletController = new WalletController(walletService);

    @Test
    void requireAuthenticatedEmail_withNullUser_throwsUnauthorized() {
        assertThrows(UnauthorizedResourceException.class, () ->
                ReflectionTestUtils.invokeMethod(walletController, "requireAuthenticatedEmail", (UserDetails) null));
    }

    @Test
    void requireAuthenticatedEmail_withBlankUsername_throwsUnauthorized() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("   ");

        assertThrows(UnauthorizedResourceException.class, () ->
                ReflectionTestUtils.invokeMethod(walletController, "requireAuthenticatedEmail", userDetails));
    }

    @Test
    void requireAuthenticatedEmail_withValidUsername_returnsEmail() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("qa@nanobank.com");

        String email = ReflectionTestUtils.invokeMethod(walletController, "requireAuthenticatedEmail", userDetails);

        assertEquals("qa@nanobank.com", email);
    }
}
