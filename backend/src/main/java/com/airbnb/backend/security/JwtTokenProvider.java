package com.airbnb.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;


    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    private String buildToken(String subject,
                              String email,
                              String roles,
                              long expirationMs) {
        Date now        = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());

        if (email != null) builder.claim("email", email);
        if (roles  != null) builder.claim("roles", roles);

        return builder.compact();
    }


    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal =
                (UserPrincipal) authentication.getPrincipal();

        String roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return buildToken(
                userPrincipal.getId().toString(),
                userPrincipal.getEmail(),
                roles,
                accessTokenExpiration
        );
    }


    public String generateAccessTokenFromUserId(Long userId,
                                                String email,
                                                String roles) {
        return buildToken(
                userId.toString(),
                email,
                roles,
                accessTokenExpiration
        );
    }


    public String generateRefreshToken(Long userId) {
        return buildToken(
                userId.toString(),
                null,
                null,
                refreshTokenExpiration
        );
    }


    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String getRolesFromToken(String token) {
        return parseClaims(token).get("roles", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT empty or null: {}", e.getMessage());
        }
        return false;
    }


    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}