package com.auth.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    // 1 Hour in milliseconds
    public static final long ACCESS_TOKEN_EXPIRATION = 3600000;
    // 2 Weeks in milliseconds
    public static final long REFRESH_TOKEN_EXPIRATION = 1209600000;

    public String generateAccessToken(String username) {
        return buildToken(new HashMap<>(), username, ACCESS_TOKEN_EXPIRATION);
    }

    public String generateRefreshToken(String username) {
        return buildToken(new HashMap<>(), username, REFRESH_TOKEN_EXPIRATION);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername != null && extractedUsername.equals(username);
    }

    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody().getSubject();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException | SignatureException e) {
            log.debug("Invalid token: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.debug("Token argument invalid: {}", e.getMessage());
            return null;
        }
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}