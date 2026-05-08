package com.inkwell.post_category_tag_service.dto;

import com.inkwell.post_category_tag_service.model.PostStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class PostRequest {
    private Long authorId;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String excerpt;
    private String featuredImageUrl;
    private PostStatus status = PostStatus.DRAFT;
    private Set<Long> categoryIds;
    private Set<Long> tagIds;
}
