package com.taskflow.service;

import com.taskflow.dto.*;
import com.taskflow.model.User;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email already registered: " + req.getEmail());
        }

        User user = User.builder()
            .name(req.getName())
            .email(req.getEmail())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(User.Role.USER)
            .build();

        User saved = userRepository.save(user);
        log.info("New user registered: email={}", saved.getEmail());

        String accessToken = jwtService.generateToken(saved);
        String refreshToken = jwtService.generateRefreshToken(saved);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .user(UserResponse.from(saved))
            .build();
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User logged in: email={}", user.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .user(UserResponse.from(user))
            .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .user(UserResponse.from(user))
            .build();
    }
}
