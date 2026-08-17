package org.paymentgateway.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JwtService handles generation, validation, and parsing of JWT tokens.
 *
 * Uses RS256 (asymmetric) signing. In production, load keys from keystore or KMS.
 *
 * OpenAI docs: https://platform.openai.com/docs
 * Java docs: https://docs.oracle.com/en/java/
 */
@Service
public class JwtService extends BaseService {

    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    @Value("${jwt.issuer:auth-service}")
    private String issuer;

    @Value("${jwt.access-token-validity-seconds:900}")
    private long accessTokenValiditySeconds;

    @PostConstruct
    public void initKeys() {
        // For demo only: generate ephemeral RSA keypair. Replace with secure key management.
        this.keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValiditySeconds);

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .addClaims(claims)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // log or rethrow as needed
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getPublicKeyPem() {
        // Optional helper to expose public key for other services to verify tokens
        return java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
}
