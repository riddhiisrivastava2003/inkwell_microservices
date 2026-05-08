package com.inkwell.comment_like_service;

import com.inkwell.comment_like_service.dto.CommentRequest;
import com.inkwell.comment_like_service.model.Comment;
import com.inkwell.comment_like_service.model.CommentStatus;
import com.inkwell.comment_like_service.repository.CommentLikeRepository;
import com.inkwell.comment_like_service.repository.CommentRepository;
import com.inkwell.comment_like_service.service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceUnitTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private WebClient webClient;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addCommentShouldSetPendingWhenModerationEnabled() {
        ReflectionTestUtils.setField(commentService, "moderationRequired", true);

        CommentRequest request = new CommentRequest();
        request.setPostId(1L);
        request.setAuthorId(2L);
        request.setContent("test");

        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment saved = commentService.addComment(request);

        assertEquals(CommentStatus.PENDING, saved.getStatus());
    }
}


