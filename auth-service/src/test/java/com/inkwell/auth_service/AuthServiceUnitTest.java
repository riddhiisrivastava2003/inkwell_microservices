package com.inkwell.auth_service;

import com.inkwell.auth_service.audit.AuditLogService;
import com.inkwell.auth_service.dto.LoginRequest;
import com.inkwell.auth_service.dto.RegisterRequest;
import com.inkwell.auth_service.exception.BadRequestException;
import com.inkwell.auth_service.model.User;
import com.inkwell.auth_service.model.UserRole;
import com.inkwell.auth_service.repository.UserFollowRepository;
import com.inkwell.auth_service.repository.UserRepository;
import com.inkwell.auth_service.service.AuthService;
import com.inkwell.auth_service.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserFollowRepository userFollowRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("reader1");
        registerRequest.setEmail("reader1@example.com");
        registerRequest.setPassword("reader123");
        registerRequest.setFullName("Reader One");
        registerRequest.setRole(UserRole.READER);
    }

    @Test
    void registerShouldReturnTokenWhenValid() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded-pass");

        User saved = new User();
        saved.setId(1L);
        saved.setEmail(registerRequest.getEmail());
        saved.setUsername(registerRequest.getUsername());
        saved.setRole(UserRole.READER);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(1L, registerRequest.getEmail(), "READER")).thenReturn("jwt-token");

        var response = authService.register(registerRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
    }

    @Test
    void loginShouldThrowWhenInvalidPassword() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("reader1@example.com");
        loginRequest.setPassword("wrong-pass");

        User existing = new User();
        existing.setId(1L);
        existing.setEmail("reader1@example.com");
        existing.setPasswordHash("encoded-pass");
        existing.setRole(UserRole.READER);
        existing.setActive(true);

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(loginRequest.getPassword(), existing.getPasswordHash())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
    }
}
