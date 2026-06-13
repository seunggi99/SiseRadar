package com.siseradar.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and verifies HS256/512 JWTs whose subject is the user id. */
@Service
public class JwtService {

  private final SecretKey key;
  private final long expirationSeconds;

  public JwtService(JwtProperties props) {
    this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    this.expirationSeconds = props.expirationSeconds();
  }

  public String generate(Long userId, String email) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("email", email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationSeconds)))
        .signWith(key)
        .compact();
  }

  /** Returns the user id from a valid token, or throws if the token is invalid/expired. */
  public Long parseUserId(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    return Long.valueOf(claims.getSubject());
  }
}
