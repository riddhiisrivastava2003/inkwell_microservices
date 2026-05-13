package com.inkwell.auth_service;

import com.inkwell.auth_service.audit.AuditLogService;
import com.inkwell.auth_service.exception.BadRequestException;
import com.inkwell.auth_service.model.User;
import com.inkwell.auth_service.model.UserFollow;
import com.inkwell.auth_service.repository.UserFollowRepository;
import com.inkwell.auth_service.repository.UserRepository;
import com.inkwell.auth_service.service.AuthService;
import com.inkwell.auth_service.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceFollowUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserFollowRepository userFollowRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @Test
    void followShouldThrowWhenSelfFollow() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.follow(5L, 5L));
        assertEquals("You cannot follow yourself", ex.getMessage());
    }

    @Test
    void followShouldReturnFollowingTrue() {
        User follower = new User();
        follower.setId(2L);
        User followee = new User();
        followee.setId(9L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(follower));
        when(userRepository.findById(9L)).thenReturn(Optional.of(followee));
        when(userFollowRepository.existsByFollowerIdAndFolloweeId(2L, 9L)).thenReturn(false);
        when(userFollowRepository.countByFolloweeId(9L)).thenReturn(3L);

        Map<String, Object> response = authService.follow(2L, 9L);
        assertEquals(true, response.get("following"));
        assertEquals(3L, response.get("followerCount"));
    }

    @Test
    void followerIdsShouldReturnDistinctList() {
        UserFollow a = new UserFollow();
        a.setFollowerId(10L);
        UserFollow b = new UserFollow();
        b.setFollowerId(11L);
        UserFollow c = new UserFollow();
        c.setFollowerId(10L);

        when(userFollowRepository.findByFolloweeId(22L)).thenReturn(List.of(a, b, c));

        List<Long> ids = authService.followerIds(22L);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(10L));
        assertTrue(ids.contains(11L));
    }
}

