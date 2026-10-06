package com.researchassistant.auth.service;

import com.researchassistant.auth.dto.LoginRequest;
import com.researchassistant.auth.dto.RegisterRequest;
import com.researchassistant.auth.security.JwtService;
import com.researchassistant.common.exception.ConflictException;
import com.researchassistant.user.entity.User;
import com.researchassistant.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);

        // Never store the password directly
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setCreatedAt(LocalDateTime.now());

        try {
            return userRepository.save(user);

        } catch (DataIntegrityViolationException e) {
            // Concurrent registration with the same email
            throw new ConflictException("Email already registered");
        }
    }

    public String login(LoginRequest request) {

        // Throws BadCredentialsException (mapped to 401) on failure
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                normalizeEmail(request.getEmail()),
                                request.getPassword()
                        )
                );

        return jwtService.generateToken(authentication.getName());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
