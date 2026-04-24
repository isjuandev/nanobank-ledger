package com.nanobank.ledger.security;

import java.util.Optional;

import com.nanobank.ledger.auth.entity.User;
import com.nanobank.ledger.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_withExistingEmail_returnsUserDetails() {
        String email = "qa@nanobank.com";
        User user = User.builder()
                .id(1L)
                .email(email)
                .password("encoded-password")
                .name("QA User")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        assertEquals(email, userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
    }

    @Test
    void loadUserByUsername_withNonExistentEmail_throwsUsernameNotFoundException() {
        String email = "missing@nanobank.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(email));
    }
}
