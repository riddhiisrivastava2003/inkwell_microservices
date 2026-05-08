package com.inkwell.auth_service.security;

import com.inkwell.auth_service.model.User;
import com.inkwell.auth_service.repository.UserRepository;
import com.inkwell.auth_service.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            response.sendRedirect(frontendBaseUrl + "/login");
            return;
        }

        Map<String, Object> attributes = oauth2User.getAttributes();
        User user = null;
        Long userId = readLongAttr(attributes, "userId");
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) {
            String email = readAttr(attributes, "email");
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null) {
            response.sendRedirect(frontendBaseUrl + "/login");
            return;
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String safeEmail = user.getEmail() == null ? "" : user.getEmail();
        String safeUsername = user.getUsername() == null ? "" : user.getUsername();
        String redirect = frontendBaseUrl + "/auth/oauth-success?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&userId=" + user.getId()
                + "&email=" + URLEncoder.encode(safeEmail, StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(safeUsername, StandardCharsets.UTF_8)
                + "&role=" + user.getRole().name();
        response.sendRedirect(redirect);
    }

    private String readAttr(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Long readLongAttr(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
