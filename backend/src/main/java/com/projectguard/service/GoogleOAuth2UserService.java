package com.projectguard.service;

import com.projectguard.entity.User;
import com.projectguard.entity.enums.AuthProvider;
import com.projectguard.entity.enums.Role;
import com.projectguard.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleOAuth2UserService
        extends DefaultOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    public GoogleOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oauth2User = super.loadUser(userRequest);

        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (googleId == null || email == null) {
            throw new OAuth2AuthenticationException(
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
                user.setRole(Role.STUDENT);
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setProviderId(googleId);
                user.setPassword(null);

            } else {
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setProviderId(googleId);
            }

            userRepository.save(user);
        }

        return oauth2User;
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