package org.paymentgateway.auth.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.paymentgateway.auth.constants.SecurityConstants;
import org.paymentgateway.auth.security.JwtUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
        JwtTokenProvider jwtTokenProvider,
        JwtUserDetailsService userDetailsService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Optional<String> jwt = parseJwt(request);

            if (jwt.isPresent() && jwtTokenProvider.validateJwtToken(jwt.get())) {
                Optional<String> username = jwtTokenProvider.getUsernameFromJwtToken(jwt.get());

                if (username.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username.get());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Cannot set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> parseJwt(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        String headerAuth = request.getHeader(SecurityConstants.AUTH_HEADER);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = headerAuth.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
            return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
        }

        return Optional.empty();
    }
}
