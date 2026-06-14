package com.siseradar.collect;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * data.go.kr connection settings. {@code baseUrl} is the RTMS root (1613000); the per-type
 * operation path is appended by the client. {@code serviceKey} is the <b>encoding</b> variant.
 */
@ConfigurationProperties(prefix = "siseradar.datagokr")
public record DataGoKrProperties(String baseUrl, String serviceKey) {}
