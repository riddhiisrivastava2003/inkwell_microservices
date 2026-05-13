package com.inkwell.post_category_tag_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagRequest {
    @NotBlank
    private String name;
}
