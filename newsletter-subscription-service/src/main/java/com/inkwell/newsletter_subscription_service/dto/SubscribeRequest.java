package com.inkwell.newsletter_subscription_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscribeRequest {
    @NotBlank
    @Email
    private String email;
    private Long userId;
    private String fullName;
    private String preferences;
}
