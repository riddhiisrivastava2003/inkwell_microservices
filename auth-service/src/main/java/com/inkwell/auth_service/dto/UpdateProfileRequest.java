package com.inkwell.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    @NotBlank
    private String fullName;

    private String bio;

    private String avatarUrl;
}
