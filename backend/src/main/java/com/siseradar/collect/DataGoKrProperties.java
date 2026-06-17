package com.siseradar.collect;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * data.go.kr connection settings. {@code baseUrl} is the RTMS root (1613000); the per-type
 * operation path is appended by the client. {@code serviceKey} is the <b>encoding</b> variant
 * (URL에 그대로 붙임); {@code serviceKeyDecoding} is the <b>decoding</b> variant (URL에 넣을 때
 * URL-encode 필요) — 진단/폴백용.
 */
@ConfigurationProperties(prefix = "siseradar.datagokr")
public record DataGoKrProperties(String baseUrl, String serviceKey, String serviceKeyDecoding) {}
