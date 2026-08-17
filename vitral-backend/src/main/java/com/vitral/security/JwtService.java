package com.vitral.security;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.vitral.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiration;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expiration = properties.expiration();
    }

    public String generateToken(String subject, String type) {
        return generateToken(subject, type, 0);
    }

    public String generateToken(String subject, String type, int authVersion) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(subject)
                .claim("type", type)
                .claim("ver", authVersion)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    public String extractSubject(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public Instant extractExpiration(String token) {
        return parse(token).getExpiration().toInstant();
    }

    public int extractAuthVersion(String token) {
        Integer version = parse(token).get("ver", Integer.class);
        return version == null ? 0 : version;
    }

    public long getExpiration() {
        return expiration;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
