package com.inkwell.auth_service.service;

import com.inkwell.auth_service.model.AuthProvider;
import com.inkwell.auth_service.model.User;
import com.inkwell.auth_service.model.UserRole;
import com.inkwell.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        Map<String, Object> attrs = oauthUser.getAttributes();

        String email = asText(attrs.get("email"));
        if (email.isBlank()) {
            email = registrationId + "_" + UUID.randomUUID() + "@oauth.local";
        }

        String defaultIdentity = email.contains("@") ? email.substring(0, email.indexOf("@")) : ("user_" + UUID.randomUUID());
        String name = asText(attrs.get("name"));
        if (name.isBlank()) {
            name = defaultIdentity;
        }
        String username = asText(attrs.get("login"));
        if (username.isBlank()) {
            username = defaultIdentity;
        }
        username = sanitizeUsername(username);

        User user = userRepository.findByEmail(email).orElseGet(User::new);
        String finalUsername = user.getId() == null
                ? ensureUniqueUsername(username)
                : user.getUsername();
        if (finalUsername == null || finalUsername.isBlank()) {
            finalUsername = ensureUniqueUsername(username);
        }
        user.setEmail(email);
        user.setFullName(name);
        user.setUsername(finalUsername);
        user.setRole(user.getRole() == null ? UserRole.READER : user.getRole());
        user.setProvider("github".equalsIgnoreCase(registrationId) ? AuthProvider.GITHUB : AuthProvider.GOOGLE);
        user.setActive(true);

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            user.setPasswordHash(UUID.randomUUID().toString());
        }

        userRepository.save(user);
        Map<String, Object> normalizedAttrs = new HashMap<>(attrs);
        normalizedAttrs.put("email", email);
        normalizedAttrs.put("name", name);
        normalizedAttrs.put("userId", user.getId());
        normalizedAttrs.put("role", user.getRole().name());

        return new DefaultOAuth2User(oauthUser.getAuthorities(), normalizedAttrs, userNameAttributeName);
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String sanitizeUsername(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        return normalized.isBlank() ? ("user" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)) : normalized;
    }

    private String ensureUniqueUsername(String baseUsername) {
        String base = sanitizeUsername(baseUsername);
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        return candidate;
    }
}
