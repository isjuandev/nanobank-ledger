package com.nanobank.ledger.auth.service;

import com.nanobank.ledger.auth.dto.AuthResponse;
import com.nanobank.ledger.auth.dto.LoginRequest;
import com.nanobank.ledger.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
