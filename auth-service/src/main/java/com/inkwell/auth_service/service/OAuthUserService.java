package com.inkwell.auth_service.service;

import com.inkwell.auth_service.model.AuthProvider;
import com.inkwell.auth_service.model.User;
import com.inkwell.auth_service.model.UserRole;
import com.inkwell.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = "github".equalsIgnoreCase(registrationId) ? AuthProvider.GITHUB : AuthProvider.GOOGLE;
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        Map<String, Object> attrs = oauthUser.getAttributes();
        String providerUserId = resolveProviderUserId(attrs, userNameAttributeName);

        String email = asText(attrs.get("email"));
        if (email.isBlank() && !providerUserId.isBlank()) {
            email = registrationId + "_" + providerUserId + "@oauth.local";
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
        if (providerUserId.isBlank()) {
            providerUserId = username;
        }
        if (email.isBlank()) {
            email = registrationId + "_" + providerUserId + "@oauth.local";
        }

        User user = null;
        if (!providerUserId.isBlank()) {
            user = userRepository.findByProviderAndProviderUserId(provider, providerUserId).orElse(null);
        }
        if (user == null) {
            user = userRepository.findByEmail(email).orElseGet(User::new);
        }
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
        user.setProvider(provider);
        user.setProviderUserId(providerUserId.isBlank() ? null : providerUserId);
        user.setActive(true);

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            // Store a BCrypt-encoded random string — OAuth users never use password login
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        }

        userRepository.save(user);
        Map<String, Object> normalizedAttrs = new HashMap<>(attrs);
        normalizedAttrs.put("email", email);
        normalizedAttrs.put("name", name);
        normalizedAttrs.put("userId", user.getId());
        normalizedAttrs.put("role", user.getRole().name());
        normalizedAttrs.put("provider", user.getProvider().name());
        normalizedAttrs.put("providerUserId", user.getProviderUserId());

        // DefaultOAuth2User requires the userNameAttributeName key to be present in the map.
        // Guarantee it is always there (especially for Google which uses 'sub').
        if (!normalizedAttrs.containsKey(userNameAttributeName) || normalizedAttrs.get(userNameAttributeName) == null) {
            normalizedAttrs.put(userNameAttributeName, providerUserId);
        }

        return new DefaultOAuth2User(oauthUser.getAuthorities(), normalizedAttrs, userNameAttributeName);
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String sanitizeUsername(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        return normalized.isBlank() ? ("user" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)) : normalized;
    }

    private String resolveProviderUserId(Map<String, Object> attrs, String userNameAttributeName) {
        // Check the provider's declared userNameAttributeName first (e.g. 'sub' for Google, 'login' for GitHub)
        String fromUserNameAttr = asText(attrs.get(userNameAttributeName));
        if (!fromUserNameAttr.isBlank()) {
            return fromUserNameAttr;
        }
        // Fallbacks for providers that use 'sub' (OIDC) or 'id' (legacy) or 'login' (GitHub)
        String fromSub = asText(attrs.get("sub"));
        if (!fromSub.isBlank()) {
            return fromSub;
        }
        String fromId = asText(attrs.get("id"));
        if (!fromId.isBlank()) {
            return fromId;
        }
        String fromLogin = asText(attrs.get("login"));
        if (!fromLogin.isBlank()) {
            return fromLogin;
        }
        return "";
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
