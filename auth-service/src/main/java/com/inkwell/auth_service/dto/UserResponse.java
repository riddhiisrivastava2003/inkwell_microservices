package com.inkwell.auth_service.dto;

import com.inkwell.auth_service.model.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String avatarUrl;
    private UserRole role;
    private boolean active;
}
