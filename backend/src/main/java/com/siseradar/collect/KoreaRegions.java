package com.siseradar.collect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The authoritative master of South Korean 시군구 (250) — 법정동 5-digit code → full name +
 * 시군구 label. Used to (1) reject out-of-country coordinates (Kakao returns sentinel codes
 * 90000/90005/90009 for non-SK points), and (2) build geocoding queries / validate geocoded
 * addresses for the map. Loaded from {@code korea-sigungu.tsv} (code\tname\tsigungu).
 */
@Component
public class KoreaRegions {

  /** lawdCd → full name ("경기 성남시 분당구"), for address-search centroid queries. */
  private final Map<String, String> names = new HashMap<>();
  /** lawdCd → 시군구 ("성남시 분당구"), for validating a geocoded address contains the right 구. */
  private final Map<String, String> sigungus = new HashMap<>();

  public KoreaRegions() {
    try (BufferedReader r =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource("korea-sigungu.tsv").getInputStream(),
                StandardCharsets.UTF_8))) {
      r.lines()
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .forEach(
              line -> {
                String[] f = line.split("\t");
                if (f.length >= 3) {
                  names.put(f[0], f[1]);
                  sigungus.put(f[0], f[2]);
                }
              });
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load korea-sigungu.tsv", e);
    }
  }

  public boolean contains(String lawdCd) {
    return names.containsKey(lawdCd);
  }

  public int size() {
    return names.size();
  }

  /** Full name ("경기 성남시 분당구") — the address-search query for the region centroid. */
  public String name(String lawdCd) {
    return names.get(lawdCd);
  }

  /**
   * The most specific 구/시/군 token (e.g. "분당구" from "성남시 분당구", "강남구", "의정부시") —
   * used to check a geocoded building's address actually falls in the expected 시군구.
   */
  public String sigunguToken(String lawdCd) {
    String s = sigungus.get(lawdCd);
    if (s == null || s.isBlank()) {
      return null;
    }
    String[] parts = s.split(" ");
    return parts[parts.length - 1];
  }
}
