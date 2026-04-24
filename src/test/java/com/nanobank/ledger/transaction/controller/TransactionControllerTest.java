package com.nanobank.ledger.transaction.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobank.ledger.exception.GlobalExceptionHandler;
import com.nanobank.ledger.exception.TransactionNotFoundException;
import com.nanobank.ledger.security.JwtAuthFilter;
import com.nanobank.ledger.security.JwtService;
import com.nanobank.ledger.transaction.dto.TransactionRequestDTO;
import com.nanobank.ledger.transaction.dto.TransactionResponseDTO;
import com.nanobank.ledger.transaction.dto.TransferRequestDTO;
import com.nanobank.ledger.transaction.entity.TransactionType;
import com.nanobank.ledger.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void getTransactions_withValidToken_returns200AndList() throws Exception {
        TransactionResponseDTO transaction = TransactionResponseDTO.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.INGRESO)
                .category("SALARIO")
                .walletId(10L)
                .walletName("Main")
                .date(LocalDateTime.of(2026, 4, 24, 10, 0))
                .build();

        when(transactionService.getTransactions(eq(10L), any(), eq("qa@nanobank.com")))
                .thenReturn(List.of(transaction));

        mockMvc.perform(get("/api/transactions")
                        .param("walletId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].category").value("SALARIO"));
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void getTransactions_withCategoryFilter_callsServiceWithFilter() throws Exception {
        when(transactionService.getTransactions(eq(20L), any(), eq("qa@nanobank.com")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/transactions")
                        .param("walletId", "20")
                        .param("category", "COMIDA"))
                .andExpect(status().isOk());

        ArgumentCaptor<com.nanobank.ledger.transaction.dto.TransactionFilterDTO> filterCaptor =
                ArgumentCaptor.forClass(com.nanobank.ledger.transaction.dto.TransactionFilterDTO.class);

        verify(transactionService, times(1)).getTransactions(eq(20L), filterCaptor.capture(), eq("qa@nanobank.com"));
        assertThat(filterCaptor.getValue().getCategory()).isEqualTo("COMIDA");
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void createTransaction_withValidBody_returns201() throws Exception {
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.GASTO)
                .category("COMIDA")
                .description("Almuerzo")
                .walletId(10L)
                .date(LocalDateTime.of(2026, 4, 24, 12, 0))
                .build();

        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .id(99L)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.GASTO)
                .category("COMIDA")
                .description("Almuerzo")
                .walletId(10L)
                .walletName("Main")
                .build();

        when(transactionService.createTransaction(any(TransactionRequestDTO.class), eq("qa@nanobank.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.walletId").value(10));
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void createTransaction_withInvalidBody_returns400() throws Exception {
        String invalidBody = """
                {
                  "amount": null,
                  "type": "GASTO",
                  "category": "COMIDA",
                  "walletId": 10
                }
                """;

                mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidBody))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("ERROR_VALIDACION"))
                        .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void transferTransaction_withValidTarget_returns200() throws Exception {
        TransferRequestDTO request = TransferRequestDTO.builder().targetWalletId(200L).build();

        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .id(7L)
                .walletId(200L)
                .category("TRANSFERENCIA")
                .type(TransactionType.GASTO)
                .amount(new BigDecimal("20.00"))
                .build();

        when(transactionService.transferTransaction(eq(7L), any(TransferRequestDTO.class), eq("qa@nanobank.com")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/transactions/7/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.walletId").value(200));
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void transferTransaction_notFound_returns404() throws Exception {
        TransferRequestDTO request = TransferRequestDTO.builder().targetWalletId(200L).build();

        when(transactionService.transferTransaction(eq(999L), any(TransferRequestDTO.class), eq("qa@nanobank.com")))
                .thenThrow(new TransactionNotFoundException("Transacción no encontrada para id: 999"));

        mockMvc.perform(patch("/api/transactions/999/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    @WithMockUser(username = "qa@nanobank.com")
    void deleteTransaction_existingId_returns204() throws Exception {
        mockMvc.perform(delete("/api/transactions/30"))
                .andExpect(status().isNoContent());

        verify(transactionService, times(1)).deleteTransaction(30L, "qa@nanobank.com");
    }
}
