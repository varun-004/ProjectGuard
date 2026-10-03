package com.projectguard.service;

import com.projectguard.dto.auth.AuthResponse;
import com.projectguard.dto.auth.LoginRequest;
import com.projectguard.dto.auth.RegisterRequest;
import com.projectguard.entity.User;
import com.projectguard.entity.enums.AuthProvider;
import com.projectguard.entity.enums.Role;
import com.projectguard.exception.DuplicateResourceException;
import com.projectguard.exception.ResourceNotFoundException;
import com.projectguard.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordService passwordService,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {

        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();

        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setName(request.getUsername().trim());
        user.setPassword(
                passwordService.encode(request.getPassword())
        );
        user.setRole(Role.STUDENT);
        user.setAuthProvider(AuthProvider.LOCAL);

        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name()
        );
    }
}