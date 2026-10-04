package com.paymentgateway.payment.config;

import com.paymentgateway.payment.client.AuthServiceClient;
import com.paymentgateway.payment.client.dto.AuthUserProfile;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Security configuration:
 * Validates incoming tokens by delegating to the external Auth microservice.
 * No JWT parsing or secret key classes exist locally in this microservice.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthServiceClient authServiceClient;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String allowedOrigins;

    public SecurityConfig(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // Allow Swagger & OpenAPI docs without token
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Allow Actuator health checks
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // All payment API endpoints require an authenticated user with a token
                .requestMatchers("/api/v1/payments/**").authenticated()
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

        private final AuthServiceClient authServiceClient;

        public TokenAuthenticationFilter(AuthServiceClient authServiceClient) {
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
