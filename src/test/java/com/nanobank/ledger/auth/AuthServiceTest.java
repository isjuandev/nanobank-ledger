package com.nanobank.ledger.auth;

import com.nanobank.ledger.auth.dto.AuthResponse;
import com.nanobank.ledger.auth.dto.LoginRequest;
import com.nanobank.ledger.auth.dto.RegisterRequest;
import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.auth.repository.UserRepository;
import com.nanobank.ledger.auth.service.impl.AuthServiceImpl;
import com.nanobank.ledger.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_success_returnsToken() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@nanobank.com")
                .password("plain-password")
                .name("Nano User")
                .build();

        when(userRepository.existsByEmail("user@nanobank.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(7L, response.getUserId());
        assertEquals("user@nanobank.com", response.getEmail());
    }

    @Test
    void login_invalidPassword_throwsException() {
        LoginRequest request = LoginRequest.builder()
                .email("user@nanobank.com")
                .password("wrong-password")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void register_existingEmail_throwsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@nanobank.com")
                .password("plain-password")
                .name("Nano User")
                .build();
        when(userRepository.existsByEmail("user@nanobank.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest request = LoginRequest.builder()
                .email("user@nanobank.com")
                .password("correct-password")
                .build();
        User user = User.builder()
                .id(3L)
                .email("user@nanobank.com")
                .password("encoded")
                .name("User")
                .build();
        when(userRepository.findByEmail("user@nanobank.com")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token-ok");

        AuthResponse response = authService.login(request);

        assertEquals("token-ok", response.getToken());
        assertEquals(3L, response.getUserId());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
