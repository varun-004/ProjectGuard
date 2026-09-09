package com.projectguard.repository;

import com.projectguard.entity.User;
import com.projectguard.entity.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderIdAndAuthProvider(
            String providerId,
            AuthProvider authProvider
    );

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}