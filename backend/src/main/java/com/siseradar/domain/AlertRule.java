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

/** An alert rule attached to a watchlist item. {@code threshold} is the % for PRICE_CHANGE_PCT. */
@Entity
@Table(
    name = "alert_rule",
    indexes = {
      @Index(name = "idx_alert_user", columnList = "user_id"),
      @Index(name = "idx_alert_watchlist", columnList = "watchlist_id")
    })
public class AlertRule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "watchlist_id", nullable = false)
  private Long watchlistId;

  @Enumerated(EnumType.STRING)
  @Column(name = "condition", nullable = false, length = 24)
  private AlertCondition condition;

  @Column(name = "threshold")
  private Double threshold;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AlertRule() {}

  public AlertRule(
      Long userId, Long watchlistId, AlertCondition condition, Double threshold, Instant createdAt) {
    this.userId = userId;
    this.watchlistId = watchlistId;
    this.condition = condition;
    this.threshold = threshold;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getWatchlistId() {
    return watchlistId;
  }

  public AlertCondition getCondition() {
    return condition;
  }

  public Double getThreshold() {
    return threshold;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
