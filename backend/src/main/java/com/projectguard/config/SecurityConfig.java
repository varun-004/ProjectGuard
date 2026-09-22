package com.projectguard.config;

import com.projectguard.security.GoogleAuthenticationSuccessHandler;
import com.projectguard.security.JwtAuthenticationFilter;
import com.projectguard.service.GoogleOAuth2UserService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final GoogleAuthenticationSuccessHandler googleSuccessHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            GoogleOAuth2UserService googleOAuth2UserService,
            GoogleAuthenticationSuccessHandler googleSuccessHandler) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.googleSuccessHandler = googleSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public OAuth2UserService<
            org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest,
            OAuth2User> oauth2UserService() {
        return googleOAuth2UserService;
    }

    // CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    // Security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .csrf(csrf -> csrf.disable())

                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public authentication APIs
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // Actuator
                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()

                        // OAuth2
                        .requestMatchers(
                                "/oauth2/**"
                        ).permitAll()

                        .requestMatchers(
                                "/login/**"
                        ).permitAll()

                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Uploaded profile photos
                        .requestMatchers(
                                "/uploads/**"
                        ).permitAll()

                        // Admin APIs
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // Student APIs
                        .requestMatchers(
                                "/api/student/**"
                        ).hasRole("STUDENT")

                        .requestMatchers(
                                "/api/students/**"
                        ).hasRole("STUDENT")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Return 401 for unauthenticated API requests
                // instead of redirecting them to Google
                .exceptionHandling(ex ->
                        ex.defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                ),
                                request ->
                                        request.getRequestURI()
                                                .startsWith("/api/")
                        )
                )

                // Google OAuth2
                .oauth2Login(oauth ->
                        oauth
                                .userInfoEndpoint(userInfo ->
                                        userInfo.userService(
                                                googleOAuth2UserService
                                        )
                                )
                                .successHandler(
                                        googleSuccessHandler
                                )
                )

                // JWT filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}