package com.siseradar.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT signing config. {@code secret} is used as raw UTF-8 key bytes (must be ≥32 bytes). */
@ConfigurationProperties(prefix = "siseradar.jwt")
public record JwtProperties(String secret, long expirationSeconds) {}
