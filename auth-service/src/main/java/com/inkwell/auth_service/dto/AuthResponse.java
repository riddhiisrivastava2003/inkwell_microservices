package com.inkwell.auth_service.dto;

import com.inkwell.auth_service.model.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String username;
    private UserRole role;
}
