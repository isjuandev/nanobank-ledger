package com.nanobank.ledger.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobank.ledger.auth.dto.AuthResponse;
import com.nanobank.ledger.auth.dto.LoginRequest;
import com.nanobank.ledger.auth.dto.RegisterRequest;
import com.nanobank.ledger.auth.service.AuthService;
import com.nanobank.ledger.exception.GlobalExceptionHandler;
import com.nanobank.ledger.security.JwtAuthFilter;
import com.nanobank.ledger.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void register_validBody_returns201WithToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@nanobank.com")
                .password("secret123")
                .name("Nuevo")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt-register-token")
                .userId(1L)
                .email("new@nanobank.com")
                .name("Nuevo")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-register-token"))
                .andExpect(jsonPath("$.email").value("new@nanobank.com"));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("qa@nanobank.com")
                .password("123456")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt-login-token")
                .userId(2L)
                .email("qa@nanobank.com")
                .name("QA")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-login-token"))
                .andExpect(jsonPath("$.userId").value(2));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("qa@nanobank.com")
                .password("wrong")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERROR_HTTP"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }
}
