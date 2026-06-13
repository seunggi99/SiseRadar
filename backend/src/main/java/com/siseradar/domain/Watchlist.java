package com.siseradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A user's watched target. {@code lawdCd} is always set (the region, or the complex's region);
 * {@code aptName} is set only for {@link WatchType#COMPLEX}.
 */
@Entity
@Table(
    name = "watchlist",
    indexes = {
      @Index(name = "idx_watchlist_user", columnList = "user_id"),
      @Index(name = "idx_watchlist_lawd", columnList = "lawd_cd")
    })
public class Watchlist {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 16)
  private WatchType type;

  @Column(name = "lawd_cd", nullable = false, length = 5)
  private String lawdCd;

  @Column(name = "apt_name")
  private String aptName;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Watchlist() {}

  public Watchlist(Long userId, WatchType type, String lawdCd, String aptName, Instant createdAt) {
    this.userId = userId;
    this.type = type;
    this.lawdCd = lawdCd;
    this.aptName = aptName;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public WatchType getType() {
    return type;
  }

  public String getLawdCd() {
    return lawdCd;
  }

  public String getAptName() {
    return aptName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
