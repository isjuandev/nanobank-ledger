package com.nanobank.ledger.auth.entity;

import java.time.LocalDateTime;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void prePersist_setsCreatedAt_whenMissing() {
        User user = User.builder()
                .id(1L)
                .email("qa@nanobank.com")
                .password("secret")
                .name("QA")
                .build();

        user.prePersist();

        assertNotNull(user.getCreatedAt());
    }

    @Test
    void prePersist_keepsCreatedAt_whenAlreadySet() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 24, 14, 0);
        User user = User.builder()
                .id(1L)
                .email("qa@nanobank.com")
                .password("secret")
                .name("QA")
                .createdAt(createdAt)
                .build();

        user.prePersist();

        assertEquals(createdAt, user.getCreatedAt());
    }

    @Test
    void getUsername_returnsEmail_andAuthoritiesAreEmpty() {
        User user = User.builder()
                .email("qa@nanobank.com")
                .password("secret")
                .name("QA")
                .build();

        assertEquals("qa@nanobank.com", user.getUsername());
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertTrue(authorities.isEmpty());
    }
}
