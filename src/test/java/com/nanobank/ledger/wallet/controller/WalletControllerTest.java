package com.nanobank.ledger.wallet.controller;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobank.ledger.exception.GlobalExceptionHandler;
import com.nanobank.ledger.exception.UnauthorizedResourceException;
import com.nanobank.ledger.exception.WalletNotFoundException;
import com.nanobank.ledger.security.JwtAuthFilter;
import com.nanobank.ledger.security.JwtService;
import com.nanobank.ledger.wallet.dto.WalletRequestDTO;
import com.nanobank.ledger.wallet.dto.WalletResponseDTO;
import com.nanobank.ledger.wallet.entity.WalletType;
import com.nanobank.ledger.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void getWallets_authenticated_returns200AndList() throws Exception {
        WalletResponseDTO wallet = WalletResponseDTO.builder()
                .id(10L)
                .name("Main Wallet")
                .type(WalletType.AHORROS)
                .balance(new BigDecimal("1200.00"))
                .transactionCount(5)
                .build();

        when(walletService.getWallets("qa@nanobank.com")).thenReturn(List.of(wallet));

        mockMvc.perform(get("/api/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Main Wallet"));
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void createWallet_validBody_returns201() throws Exception {
        WalletRequestDTO request = WalletRequestDTO.builder()
                .name("Viajes")
                .type(WalletType.AHORROS)
                .build();

        WalletResponseDTO response = WalletResponseDTO.builder()
                .id(22L)
                .name("Viajes")
                .type(WalletType.AHORROS)
                .balance(new BigDecimal("0.00"))
                .transactionCount(0)
                .build();

        when(walletService.createWallet(any(WalletRequestDTO.class), eq("qa@nanobank.com"))).thenReturn(response);

        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(22))
                .andExpect(jsonPath("$.name").value("Viajes"));
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void createWallet_blankName_returns400() throws Exception {
        String invalidBody = """
                {
                  "name": "   ",
                  "type": "AHORROS"
                }
                """;

                mockMvc.perform(post("/api/wallets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidBody))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("ERROR_VALIDACION"))
                        .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void getWalletById_exists_returns200() throws Exception {
        WalletResponseDTO response = WalletResponseDTO.builder()
                .id(5L)
                .name("Personal")
                .type(WalletType.GASTOS)
                .balance(new BigDecimal("400.00"))
                .transactionCount(2)
                .build();

        when(walletService.getWalletById(5L, "qa@nanobank.com")).thenReturn(response);

        mockMvc.perform(get("/api/wallets/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.type").value("GASTOS"));
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void getWalletById_notExists_returns404() throws Exception {
        when(walletService.getWalletById(404L, "qa@nanobank.com"))
                .thenThrow(new WalletNotFoundException("Billetera no encontrada para id: 404"));

        mockMvc.perform(get("/api/wallets/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void deleteWallet_exists_returns204() throws Exception {
        mockMvc.perform(delete("/api/wallets/8"))
                .andExpect(status().isNoContent());

        verify(walletService, times(1)).deleteWallet(8L, "qa@nanobank.com");
    }

    @Test
    void deleteWallet_unauthorized_returns403() throws Exception {
        doThrow(new UnauthorizedResourceException("Usuario no autenticado"))
                .when(walletService).deleteWallet(9L, null);

        mockMvc.perform(delete("/api/wallets/9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROHIBIDO"));
    }
}
