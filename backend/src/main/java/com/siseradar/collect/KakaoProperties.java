package com.siseradar.collect;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Kakao Local REST key (server-side only). */
@ConfigurationProperties(prefix = "siseradar.kakao")
public record KakaoProperties(String restKey) {}
