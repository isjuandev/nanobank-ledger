package com.nanobank.ledger.security;

import java.lang.reflect.Method;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String TEST_SECRET_BASE64 =
            Base64.getEncoder().encodeToString("0123456789ABCDEF0123456789ABCDEF".getBytes());

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET_BASE64);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 60_000L);

        userDetails = User.withUsername("qa@nanobank.com")
                .password("encoded-password")
                .authorities("USER")
                .build();
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
    }

    @Test
    void generateToken_containsCorrectUsername() {
        String token = jwtService.generateToken(userDetails);

        assertEquals("qa@nanobank.com", jwtService.extractUsername(token));
    }

    @Test
    void extractUsername_returnsCorrectSubject() {
        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertEquals(userDetails.getUsername(), username);
    }

    @Test
    void isTokenValid_withValidTokenAndMatchingUser_returnsTrue() {
        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1L);
        String expiredToken = jwtService.generateToken(userDetails);

        assertThrows(Exception.class, () -> jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    void isTokenValid_withWrongUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails);
        UserDetails wrongUser = User.withUsername("other@nanobank.com")
                .password("encoded-password")
                .authorities("USER")
                .build();

        assertFalse(jwtService.isTokenValid(token, wrongUser));
    }

    @Test
    void isTokenExpired_withFreshToken_returnsFalse() throws Exception {
        String token = jwtService.generateToken(userDetails);

        Method method = JwtService.class.getDeclaredMethod("isTokenExpired", String.class);
        method.setAccessible(true);
        boolean expired = (boolean) method.invoke(jwtService, token);

        assertFalse(expired);
    }
}
