package com.playville.crm.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long      expiryMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}")    String secret,
            @Value("${app.jwt.expiry-ms}") long   expiryMs) {
        this.key      = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryMs;
    }

    public String generateToken(StaffPrincipal principal) {
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("staffId",    principal.getId())
                .claim("branchId",   principal.getBranchId())
                .claim("branchCode", principal.getBranchCode())
                .claim("role",       principal.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public Integer getBranchIdFromToken(String token) {
        return parseToken(token).get("branchId", Integer.class);
    }
}