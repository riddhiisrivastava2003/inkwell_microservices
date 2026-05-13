package com.inkwell.post_category_tag_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSummaryResponse {
    private Long postId;
    private Long authorId;
    private String title;
    private String slug;
    private String status;
}
