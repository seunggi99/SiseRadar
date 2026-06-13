package com.siseradar.collect;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * data.go.kr connection settings. {@code serviceKey} is the <b>encoding</b> variant
 * (already percent-encoded) and is passed through verbatim to avoid double-encoding.
 */
@ConfigurationProperties(prefix = "siseradar.datagokr")
public record DataGoKrProperties(String baseUrl, String serviceKey) {}
