package org.paymentgateway.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.paymentgateway.user.client.AuthenticationServiceClient;
import org.paymentgateway.user.client.dto.AuthUserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Configures web security for the application and exposes required documentation endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthenticationServiceClient authServiceClient;

    public SecurityConfig(AuthenticationServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Allow Swagger & OpenAPI docs without token
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Allow Actuator health checks
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // All payment API endpoints require an authenticated user with a token
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new TokenAuthenticationFilter(authServiceClient), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Intercepts the Authorization header, validates the token against the Auth microservice,
     * and populates Spring Security's SecurityContextHolder.
     */
    public static class TokenAuthenticationFilter extends OncePerRequestFilter {

        private final AuthenticationServiceClient authServiceClient;

        public TokenAuthenticationFilter(AuthenticationServiceClient authServiceClient) {
            this.authServiceClient = authServiceClient;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    Optional<AuthUserProfile> profileOpt = authServiceClient.getAuthenticatedUser(authHeader);

                    if (profileOpt.isPresent()) {
                        AuthUserProfile profile = profileOpt.get();

                        List<SimpleGrantedAuthority> authorities = profile.roles() != null
                                ? profile.roles().stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList())
                                : Collections.emptyList();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(profile, authHeader, authorities);

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("User '{}' (ID: {}) successfully authenticated via Auth microservice",
                                profile.username(), profile.id());
                    } else {
                        log.warn("Token validation failed with Auth microservice for path: {}", request.getRequestURI());
                    }
                } catch (Exception ex) {
                    log.error("Error during external auth verification: {}", ex.getMessage());
                }
            }

            filterChain.doFilter(request, response);
        }
    }
}
