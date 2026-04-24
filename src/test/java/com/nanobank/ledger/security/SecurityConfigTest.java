package com.nanobank.ledger.security;

import java.util.List;

import com.nanobank.ledger.auth.dto.AuthResponse;
import com.nanobank.ledger.auth.dto.LoginRequest;
import com.nanobank.ledger.auth.dto.RegisterRequest;
import com.nanobank.ledger.auth.controller.AuthController;
import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.auth.service.AuthService;
import com.nanobank.ledger.wallet.controller.WalletController;
import com.nanobank.ledger.wallet.dto.WalletResponseDTO;
import com.nanobank.ledger.wallet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityConfigTest.TestApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
                "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
                "jwt.expiration-ms=60000"
        }
)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicEndpoints_doNotRequireAuth() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("dummy-token")
                .userId(1L)
                .email("qa@nanobank.com")
                .name("QA")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"qa@nanobank.com\",\"password\":\"123456\"}"))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"qa@nanobank.com\",\"password\":\"123456\",\"name\":\"QA\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void protectedEndpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/api/wallets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoints_withValidToken_return2xx() throws Exception {
        String token = "valid-jwt-token";
        User user = User.builder()
                .id(1L)
                .email("qa@nanobank.com")
                .password("encoded")
                .name("QA")
                .build();

        WalletResponseDTO wallet = WalletResponseDTO.builder()
                .id(10L)
                .name("Main Wallet")
                .balance(new java.math.BigDecimal("100.00"))
                .type(com.nanobank.ledger.wallet.entity.WalletType.AHORROS)
                .build();

        when(jwtService.extractUsername(token)).thenReturn("qa@nanobank.com");
        when(userDetailsService.loadUserByUsername("qa@nanobank.com")).thenReturn(user);
        when(jwtService.isTokenValid(eq(token), eq(user))).thenReturn(true);
        when(walletService.getWallets("qa@nanobank.com")).thenReturn(List.of(wallet));

        mockMvc.perform(get("/api/wallets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, JwtAuthFilter.class, AuthController.class, WalletController.class})
    static class TestApplication {
    }
}
