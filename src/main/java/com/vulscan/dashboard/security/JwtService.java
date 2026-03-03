package com.vulscan.dashboard.security;

import com.vulscan.dashboard.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessMinutes(), ChronoUnit.MINUTES);

        List<String> roles = user.getRoles().stream().map(role -> role.getName().name()).toList();

        return Jwts.builder()
                .subject(user.getEmail())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("type", "access")
                .claim("uid", user.getId())
                .claim("roles", roles)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getRefreshDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(user.getEmail())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("type", "refresh")
                .claim("uid", user.getId())
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenType(String token, String tokenType) {
        Object type = parseClaims(token).get("type");
        return tokenType.equals(type);
    }

    public long getAccessExpiresInSeconds() {
        return jwtProperties.getAccessMinutes() * 60;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
