package com.nanobank.ledger.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleWalletNotFound_returns404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/test-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleTransactionNotFound_returns404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/test-transaction-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleUnauthorized_returns403WithErrorResponse() throws Exception {
        mockMvc.perform(get("/test-unauthorized"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROHIBIDO"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleValidation_withBlankField_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERROR_VALIDACION"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.name").value("name is required"));
    }

    @Test
    void handleInsufficientBalance_returns422() throws Exception {
        mockMvc.perform(get("/test-insufficient-balance"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SALDO_INSUFICIENTE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleGenericException_returns500WithoutStacktrace() throws Exception {
        mockMvc.perform(get("/test-generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO_SERVIDOR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(content().string(not(containsString("at com."))));
    }

    @Test
    void handleWalletDeletionNotAllowed_returns409WithErrorResponse() throws Exception {
        mockMvc.perform(get("/test-wallet-deletion-not-allowed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BILLETERA_CON_TRANSACCIONES"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @RestController
    public static class TestController {

        @GetMapping("/test-not-found")
        public void notFound() {
            throw new WalletNotFoundException("Wallet 99 not found");
        }

        @GetMapping("/test-transaction-not-found")
        public void transactionNotFound() {
            throw new TransactionNotFoundException("Transaction 99 not found");
        }

        @GetMapping("/test-unauthorized")
        public void unauthorized() {
            throw new UnauthorizedResourceException("Forbidden action");
        }

        @PostMapping("/test-validation")
        public void validation(@Valid @RequestBody ValidationRequest request) {
            // Intentionally empty: validation happens before method body.
        }

        @GetMapping("/test-insufficient-balance")
        public void insufficientBalance() {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        @GetMapping("/test-generic")
        public void generic() {
            throw new RuntimeException("Unexpected error");
        }

        @GetMapping("/test-wallet-deletion-not-allowed")
        public void walletDeletionNotAllowed() {
            throw new WalletDeletionNotAllowedException("Wallet has associated transactions");
        }
    }

    record ValidationRequest(
            @NotBlank(message = "name is required")
            String name
    ) {
    }
}
