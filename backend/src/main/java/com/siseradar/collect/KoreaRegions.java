package com.siseradar.collect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The authoritative set of South Korean 시군구 법정동 5-digit codes (250). Used to reject
 * out-of-country coordinates: Kakao returns sentinel codes (90000 일본 / 90005 북한·중국 /
 * 90009 바다) for non-SK points, which must not be treated as a valid region.
 */
@Component
public class KoreaRegions {

  private final Set<String> codes;

  public KoreaRegions() {
    try (BufferedReader r =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource("korea-sigungu-codes.txt").getInputStream(),
                StandardCharsets.UTF_8))) {
      this.codes =
          r.lines().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load korea-sigungu-codes.txt", e);
    }
  }

  public boolean contains(String lawdCd) {
    return codes.contains(lawdCd);
  }

  public int size() {
    return codes.size();
  }
}
