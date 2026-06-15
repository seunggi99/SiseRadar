package com.siseradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Cached AI 시장 요약 for a (region, property type, trade type, period). The LLM is called only
 * when there is no cache, the underlying numbers changed (basisJson differs), or the TTL elapsed —
 * never per request.
 */
@Entity
@Table(
    name = "region_insight",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_region_insight",
            columnNames = {"lawd_cd", "property_type", "trade_type", "period"}))
public class RegionInsight {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "lawd_cd", nullable = false, length = 5)
  private String lawdCd;

  @Column(name = "property_type", nullable = false, length = 20)
  private String propertyType;

  @Column(name = "trade_type", nullable = false, length = 10)
  private String tradeType;

  /** Resolved data range "fromYm-toYm" — part of the cache key. */
  @Column(name = "period", nullable = false, length = 20)
  private String period;

  @Lob
  @Column(name = "summary", nullable = false)
  private String summary;

  /** JSON of the numbers the summary was grounded on — to detect data changes + show as basis. */
  @Lob
  @Column(name = "basis_json", nullable = false)
  private String basisJson;

  /** "ai" (LLM) or "fallback" (template) — so we don't cache a fallback as if it were AI. */
  @Column(name = "source", nullable = false, length = 12)
  private String source;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  protected RegionInsight() {}

  public RegionInsight(
      String lawdCd,
      String propertyType,
      String tradeType,
      String period,
      String summary,
      String basisJson,
      String source) {
    this.lawdCd = lawdCd;
    this.propertyType = propertyType;
    this.tradeType = tradeType;
    this.period = period;
    this.summary = summary;
    this.basisJson = basisJson;
    this.source = source;
    this.generatedAt = Instant.now();
  }

  public void refresh(String summary, String basisJson, String source) {
    this.summary = summary;
    this.basisJson = basisJson;
    this.source = source;
    this.generatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getSummary() {
    return summary;
  }

  public String getBasisJson() {
    return basisJson;
  }

  public String getSource() {
    return source;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }
}
