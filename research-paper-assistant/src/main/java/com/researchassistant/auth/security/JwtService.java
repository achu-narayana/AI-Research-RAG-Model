package com.researchassistant.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log =
            LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {

        if (secret == null || secret.isBlank()) {

            // No secret configured: generate a random 256-bit key.
            this.signingKey = Jwts.SIG.HS256.key().build();

            log.warn("JWT_SECRET is not set. Using a randomly generated "
                    + "signing key; issued tokens will not survive a restart.");

        } else {

            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "JWT_SECRET must be at least 32 bytes (256 bits) long");
            }

            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }

        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + expirationMs
        );

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies signature and expiry and returns the subject.
     * Throws JwtException / IllegalArgumentException when the
     * token is invalid or expired.
     */
    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {

        try {
            extractEmail(token);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
