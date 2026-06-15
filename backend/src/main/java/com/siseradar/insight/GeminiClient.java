package com.siseradar.insight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thin client for Gemini's generateContent (free Flash tier). Returns the generated text, or null
 * when no key is configured / the call ultimately fails — callers fall back gracefully. Thinking is
 * disabled (thinkingBudget=0) so the token budget yields the answer, not internal reasoning. 429
 * (rate limit) is retried with exponential backoff.
 */
@Component
public class GeminiClient {

  private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
  private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
  private static final int MAX_RETRIES = 3;

  private final RestClient http = RestClient.create();
  private final GeminiProperties props;

  public GeminiClient(GeminiProperties props) {
    this.props = props;
  }

  public boolean isConfigured() {
    return props.apiKey() != null && !props.apiKey().isBlank();
  }

  /** Generate text for a single user prompt, or null on no-key / repeated failure. */
  public String generate(String prompt) {
    if (!isConfigured()) {
      return null;
    }
    String model = (props.model() == null || props.model().isBlank()) ? "gemini-2.5-flash" : props.model();
    String url = BASE + model + ":generateContent?key=" + props.apiKey();
    GenerateRequest body =
        new GenerateRequest(
            List.of(new Content("user", List.of(new Part(prompt)))),
            new GenerationConfig(0.2, 320, new ThinkingConfig(0)));

    long backoffMs = 500;
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        GenerateResponse res =
            http.post()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(GenerateResponse.class);
        return firstText(res);
      } catch (RestClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        // 429(rate limit) / 503(temporary overload) are transient → exponential backoff retry
        if ((status.value() == 429 || status.value() == 503) && attempt < MAX_RETRIES) {
          sleep(backoffMs);
          backoffMs *= 2;
          continue;
        }
        log.warn("Gemini call failed ({}). {}", status, e.getMessage());
        return null;
      } catch (RuntimeException e) {
        log.warn("Gemini call error: {}", e.getMessage());
        return null;
      }
    }
    return null;
  }

  private static String firstText(GenerateResponse res) {
    if (res == null || res.candidates == null || res.candidates.isEmpty()) {
      return null;
    }
    Content c = res.candidates.get(0).content;
    if (c == null || c.parts == null || c.parts.isEmpty() || c.parts.get(0).text == null) {
      return null;
    }
    String text = c.parts.get(0).text.strip();
    return text.isEmpty() ? null : text;
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  // ── request / response shapes ──────────────────────────────────────────────
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record GenerateRequest(List<Content> contents, GenerationConfig generationConfig) {}

  record Content(String role, List<Part> parts) {}

  record Part(String text) {}

  record GenerationConfig(double temperature, int maxOutputTokens, ThinkingConfig thinkingConfig) {}

  record ThinkingConfig(int thinkingBudget) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class GenerateResponse {
    public List<Candidate> candidates;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Candidate {
    public Content content;
  }
}
