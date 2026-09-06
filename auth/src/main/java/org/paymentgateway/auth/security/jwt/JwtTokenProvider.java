package org.paymentgateway.auth.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.paymentgateway.auth.constants.SecurityConstants;
import org.paymentgateway.auth.security.JwtUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final String jwtSecret;
    private final long jwtExpirationMs;
    private final SecretKey key;

    public JwtTokenProvider(
        @Value("${app.jwt.secret}") String jwtSecret,
        @Value("${app.jwt.access-token-expiration-ms}") long jwtExpirationMs
    ) {
        this.jwtSecret = Objects.requireNonNullElse(jwtSecret, "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        this.jwtExpirationMs = jwtExpirationMs > 0 ? jwtExpirationMs : 900000;
        this.key = getSigningKey(this.jwtSecret);
    }

    private SecretKey getSigningKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserDetails userPrincipal)) {
            throw new IllegalArgumentException("Authentication principal must be a non-null CustomUserDetails");
        }
        return generateTokenFromUserDetails(userPrincipal);
    }

    public String generateTokenFromUserDetails(JwtUserDetails userPrincipal) {
        if (userPrincipal == null) {
            throw new IllegalArgumentException("User principal cannot be null");
        }

        List<String> roles = userPrincipal.getAuthorities() != null
            ? userPrincipal.getAuthorities().stream()
                .filter(Objects::nonNull)
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
            : Collections.emptyList();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
            .subject(Objects.requireNonNullElse(userPrincipal.getUsername(), ""))
            .claim(SecurityConstants.CLAIM_USER_ID, userPrincipal.getId())
            .claim(SecurityConstants.CLAIM_EMAIL, Objects.requireNonNullElse(userPrincipal.getEmail(), ""))
            .claim(SecurityConstants.CLAIM_ROLES, roles)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }

    public Optional<String> getUsernameFromJwtToken(String token) {
        return parseClaims(token)
            .map(Claims::getSubject)
            .filter(StringUtils::hasText);
    }

    public Optional<Long> getUserIdFromJwtToken(String token) {
        return parseClaims(token)
            .flatMap(claims -> {
                Object userId = claims.get(SecurityConstants.CLAIM_USER_ID);
                return userId instanceof Number number
                    ? Optional.of(number.longValue())
                    : Optional.empty();
            });
    }


    public List<String> getRolesFromJwtToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Collections.emptyList();
        }
        Optional<Claims> claims = parseClaims(token);
        if (claims.isEmpty()) {
            return Collections.emptyList();
        }

        Object rolesObj = claims.get().get(SecurityConstants.CLAIM_ROLES);
        if (rolesObj instanceof List<?> list) {
            return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Optional<Claims> parseClaims(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        return Optional.of(
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
        );
    }

    public boolean validateJwtToken(String authToken) {
        if (!StringUtils.hasText(authToken)) {
            return false;
        }
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseClaims(token)
                .orElseThrow(() -> new IllegalArgumentException("JWT claims are unavailable"));
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Get token expiration time remaining in seconds
     */
    public long getExpirationTimeRemaining(String token) {
        try {
            Claims claims = parseClaims(token)
                .orElseThrow(() -> new IllegalArgumentException("JWT claims are unavailable"));
            long expirationTimeMs = claims.getExpiration().getTime();
            long currentTimeMs = System.currentTimeMillis();
            return (expirationTimeMs - currentTimeMs) / 1000;
        } catch (Exception e) {
            return 0;
        }
    }

}
