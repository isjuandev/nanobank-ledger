package com.nanobank.ledger.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withValidBearerToken_setsSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        UserDetails userDetails = User.withUsername("qa@nanobank.com")
                .password("encoded-password")
                .authorities("USER")
                .build();

        SecurityContext mockedContext = mock(SecurityContext.class);
        when(mockedContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(mockedContext);

        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("qa@nanobank.com");
        when(userDetailsService.loadUserByUsername("qa@nanobank.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(mockedContext).setAuthentication(org.mockito.ArgumentMatchers.any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoBearerToken_doesNotSetSecurityContext() throws ServletException, IOException {
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        SecurityContext mockedContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(mockedContext);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(mockedContext, times(0)).setAuthentication(org.mockito.ArgumentMatchers.any());
        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_withMalformedToken_doesNotSetSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer malformed");

        SecurityContext mockedContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(mockedContext);

        doThrow(new RuntimeException("Invalid token")).when(jwtService).extractUsername("malformed");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(mockedContext, times(0)).setAuthentication(org.mockito.ArgumentMatchers.any());
        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_alwaysCallsFilterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext());
    }

    @Test
    void doFilterInternal_withNullUsername_skipsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer token.without.username");

        SecurityContext mockedContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(mockedContext);

        when(jwtService.extractUsername("token.without.username")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
        verify(mockedContext, never()).setAuthentication(org.mockito.ArgumentMatchers.any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer invalid.jwt.token");

        UserDetails userDetails = User.withUsername("qa@nanobank.com")
                .password("encoded")
                .authorities("USER")
                .build();

        SecurityContext mockedContext = mock(SecurityContext.class);
        when(mockedContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(mockedContext);

        when(jwtService.extractUsername("invalid.jwt.token")).thenReturn("qa@nanobank.com");
        when(userDetailsService.loadUserByUsername("qa@nanobank.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid.jwt.token", userDetails)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(mockedContext, never()).setAuthentication(org.mockito.ArgumentMatchers.any());
        verify(filterChain).doFilter(request, response);
    }
}
