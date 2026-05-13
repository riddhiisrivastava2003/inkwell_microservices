package com.inkwell.post_category_tag_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {
    @NotBlank
    private String name;
    private String description;
    private Long parentCategoryId;
}
