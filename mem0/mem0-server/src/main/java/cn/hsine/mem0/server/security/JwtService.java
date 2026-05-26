package cn.hsine.mem0.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Service for JWT token generation and validation.
 *
 * @author MoBai

 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiration;
    private final long refreshExpiration;
    private final String issuer;

    public JwtService(
        @Value("${jwt.secret:change-me-in-production-use-at-least-32-chars}") String secret,
        @Value("${jwt.expiration:86400000}") long expiration,
        @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration,
        @Value("${jwt.issuer:mem0}") String issuer
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
        this.issuer = issuer;
    }

    /**
     * Generates a JWT token for the given user.
     *
     * @param userId the user ID
     * @param email the user email
     * @return the JWT token
     */
    public String generateToken(String userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuer(issuer)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(signingKey)
            .compact();
    }

    /**
     * Generates a refresh token for the given user.
     *
     * @param userId the user ID
     * @param email the user email
     * @return the refresh token
     */
    public String generateRefreshToken(String userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("type", "refresh")
            .issuer(issuer)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(signingKey)
            .compact();
    }

    /**
     * Validates a JWT token and returns the claims.
     *
     * @param token the JWT token
     * @return the claims
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public String getUserId(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Extracts the email from a JWT token.
     *
     * @param token the JWT token
     * @return the email
     */
    public String getEmail(String token) {
        Claims claims = validateToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Checks if a token is a refresh token.
     *
     * @param token the JWT token
     * @return true if it is a refresh token
     */
    public boolean isRefreshToken(String token) {
        Claims claims = validateToken(token);
        return "refresh".equals(claims.get("type", String.class));
    }
}
