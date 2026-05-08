package com.inkwell.post_category_tag_service;

import com.inkwell.post_category_tag_service.exception.BadRequestException;
import com.inkwell.post_category_tag_service.model.Post;
import com.inkwell.post_category_tag_service.model.SavedPost;
import com.inkwell.post_category_tag_service.repository.*;
import com.inkwell.post_category_tag_service.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceSavedPostUnitTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private PostViewSessionRepository postViewSessionRepository;
    @Mock
    private SavedPostRepository savedPostRepository;
    @Mock
    private WebClient webClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PostService postService;

    @Test
    void savePostShouldThrowWhenUserMissing() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> postService.savePost(1L, null));
        assertEquals("userId is required", ex.getMessage());
    }

    @Test
    void saveStatusShouldReturnTrueWhenSaved() {
        when(savedPostRepository.existsByUserIdAndPostId(7L, 11L)).thenReturn(true);
        Map<String, Object> response = postService.saveStatus(11L, 7L);
        assertEquals(true, response.get("saved"));
    }

    @Test
    void savedPostsShouldReturnInSavedOrder() {
        SavedPost s1 = new SavedPost();
        s1.setPostId(100L);
        SavedPost s2 = new SavedPost();
        s2.setPostId(200L);
        when(savedPostRepository.findByUserIdOrderBySavedAtDesc(3L)).thenReturn(List.of(s1, s2));

        Post p1 = new Post();
        p1.setId(100L);
        p1.setTitle("First");
        Post p2 = new Post();
        p2.setId(200L);
        p2.setTitle("Second");
        when(postRepository.findAllById(List.of(100L, 200L))).thenReturn(List.of(p1, p2));

        List<Post> saved = postService.savedPosts(3L);
        assertEquals(2, saved.size());
        assertEquals(100L, saved.get(0).getId());
        assertEquals(200L, saved.get(1).getId());
    }

    @Test
    void savePostShouldReturnSavedTrueWhenPostExists() {
        Post post = new Post();
        post.setId(44L);
        when(postRepository.findById(44L)).thenReturn(Optional.of(post));
        when(savedPostRepository.existsByUserIdAndPostId(5L, 44L)).thenReturn(false);

        Map<String, Object> response = postService.savePost(44L, 5L);
        assertEquals(true, response.get("saved"));
    }
}



