package com.inkwell.comment_like_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {
    @NotNull
    private Long postId;

    private Long authorId;

    private Long parentCommentId;

    @NotBlank
    private String content;
}
