package com.inkwell.auth_service.dto;

import com.inkwell.auth_service.model.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeRoleRequest {
    private UserRole role;
}
