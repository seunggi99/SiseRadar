package com.siseradar.alert;

import com.siseradar.domain.AlertCondition;
import com.siseradar.domain.AlertRule;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class AlertDtos {

  private AlertDtos() {}

  public record AlertRuleRequest(
      @NotNull Long watchlistId, @NotNull AlertCondition condition, Double threshold) {}

  public record AlertRuleResponse(
      Long id, Long watchlistId, AlertCondition condition, Double threshold, Instant createdAt) {

    public static AlertRuleResponse from(AlertRule r) {
      return new AlertRuleResponse(
          r.getId(), r.getWatchlistId(), r.getCondition(), r.getThreshold(), r.getCreatedAt());
    }
  }
}
