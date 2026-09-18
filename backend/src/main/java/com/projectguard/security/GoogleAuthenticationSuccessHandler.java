package com.projectguard.security;

import com.projectguard.entity.User;
import com.projectguard.entity.enums.AuthProvider;
import com.projectguard.entity.enums.Role;
import com.projectguard.repository.UserRepository;
import com.projectguard.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleAuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GoogleAuthenticationSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User =
                (OAuth2User) authentication.getPrincipal();

        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (googleId == null || email == null) {
            throw new IllegalStateException(
                    "Google account information is missing"
            );
        }

        User user = userRepository
                .findByProviderIdAndAuthProvider(
                        googleId,
                        AuthProvider.GOOGLE
                )
                .orElse(null);

        if (user == null) {

            user = userRepository
                    .findByEmail(email)
                    .orElse(null);

            if (user == null) {

                user = new User();

                user.setUsername(createUniqueUsername(name, email));
                user.setEmail(email);
                user.setPassword(null);
                user.setRole(Role.STUDENT);
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setProviderId(googleId);

            } else {

                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setProviderId(googleId);
            }

            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole().name()
        );

        String frontendRedirectUrl = "http://localhost:5173/oauth2/redirect" +
                "?token=" + token +
                "&username=" + user.getUsername() +
                "&role=" + user.getRole().name();

        getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);
    }

    private String createUniqueUsername(String name, String email) {

        String baseUsername;

        if (name != null && !name.isBlank()) {

            baseUsername = name
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "");

        } else {

            baseUsername = email
                    .substring(0, email.indexOf("@"))
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
        }

        if (baseUsername.isBlank()) {
            baseUsername = "googleuser";
        }

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}