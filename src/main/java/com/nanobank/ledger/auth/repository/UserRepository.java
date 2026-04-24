package com.nanobank.ledger.auth.repository;

import java.util.Optional;

import com.nanobank.ledger.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
