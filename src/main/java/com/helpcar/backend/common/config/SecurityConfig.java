package com.helpcar.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Baseline security for the API.
 *
 * <p>The service is a stateless JSON API consumed by a mobile client, so there is no
 * session and no CSRF token exchange to protect: every request must carry its own
 * credentials. Endpoints are deny-by-default — only health probes, the ping endpoint
 * and the API docs are public.
 *
 * <p>JWT issuance and the authentication filter arrive with the identity module; until
 * then any non-public endpoint correctly answers {@code 401}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/ping",
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ENDPOINTS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // Answer 401 instead of redirecting to a login page that does not exist.
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    /**
     * BCrypt is deliberate: passwords are hashed server-side with a per-hash salt and a
     * tunable work factor. The old mobile app posted plaintext credentials over HTTP —
     * this is half of closing that hole, TLS termination is the other half.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
