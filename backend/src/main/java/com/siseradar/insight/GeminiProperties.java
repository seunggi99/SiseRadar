package com.siseradar.insight;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Google Gemini (Generative Language API) settings — free Flash tier. */
@ConfigurationProperties(prefix = "siseradar.gemini")
public record GeminiProperties(String apiKey, String model) {}
