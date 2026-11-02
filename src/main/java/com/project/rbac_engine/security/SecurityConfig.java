package com.project.rbac_engine.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.time.Instant;

/**
 * Authentication-only security configuration.
 *
 * RULE: this class contains ONLY authentication-level decisions (who can call the API).
 * It must NEVER contain role or permission checks — those belong solely in
 * CustomPermissionEvaluator, invoked through @PreAuthorize("hasPermission(...)").
 *
 * Spring Boot auto-detects the CustomUserDetailsService @Service bean and the
 * PasswordEncoder bean below and wires them into DaoAuthenticationProvider automatically,
 * so we do not need to build an AuthenticationManager manually.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt is the industry-standard adaptive password hashing algorithm.
     * Exposed as a bean so DataSeeder (and tests) can inject it for encoding seed passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF: this is a stateless REST API — no browser sessions, no cookies.
            .csrf(csrf -> csrf.disable())

            // No server-side sessions: every request must carry credentials (HTTP Basic).
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // HTTP Basic with a custom entry point so unauthenticated requests get a
            // consistent JSON 401 body instead of Spring's default HTML error page.
            .httpBasic(basic -> basic.authenticationEntryPoint(unauthorizedEntryPoint()))

            .authorizeHttpRequests(auth -> auth
                // Allow the H2 web console in development without credentials.
                // IMPORTANT: remove or restrict this for any non-development environment.
                .requestMatchers("/h2-console/**").permitAll()
                // Every other endpoint requires a valid username + password.
                // WHAT the user may do is decided by @PreAuthorize, not here.
                .anyRequest().authenticated()
            )

            // H2 console renders inside an iframe; Spring Security blocks iframes by default.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * Returns a consistent JSON error body for unauthenticated requests (HTTP 401).
     * Written inline (not in GlobalExceptionHandler) because Spring Security's filter
     * chain runs before the DispatcherServlet, so @RestControllerAdvice cannot catch it.
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                org.springframework.security.core.AuthenticationException ex) -> {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized", ex.getMessage(), request.getRequestURI());
        };
    }

    /**
     * Writes a JSON error body that matches the shape GlobalExceptionHandler will also use.
     * Keeping the format identical means callers see a consistent error contract.
     */
    static void writeJsonError(HttpServletResponse response,
                               int status,
                               String error,
                               String message,
                               String path) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        // Escape double quotes in message to keep JSON valid.
        String safeMessage = message == null ? "" : message.replace("\"", "'");

        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), status, error, safeMessage, path);

        response.getWriter().write(body);
    }
}
