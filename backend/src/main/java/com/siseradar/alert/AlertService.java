package com.siseradar.alert;

import com.siseradar.alert.AlertDtos.AlertRuleRequest;
import com.siseradar.alert.AlertDtos.AlertRuleResponse;
import com.siseradar.domain.AlertCondition;
import com.siseradar.domain.AlertRule;
import com.siseradar.domain.Watchlist;
import com.siseradar.repository.AlertRuleRepository;
import com.siseradar.repository.WatchlistRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertService {

  private final AlertRuleRepository alertRules;
  private final WatchlistRepository watchlists;

  public AlertService(AlertRuleRepository alertRules, WatchlistRepository watchlists) {
    this.alertRules = alertRules;
    this.watchlists = watchlists;
  }

  public List<AlertRuleResponse> list(Long userId) {
    return alertRules.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(AlertRuleResponse::from)
        .toList();
  }

  public AlertRuleResponse create(Long userId, AlertRuleRequest req) {
    // the rule may only attach to the caller's own watchlist item
    Watchlist w =
        watchlists
            .findById(req.watchlistId())
            .filter(item -> item.getUserId().equals(userId))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "관심목록 항목을 찾을 수 없습니다"));

    Double threshold = req.threshold();
    if (req.condition() == AlertCondition.PRICE_CHANGE_PCT
        && (threshold == null || threshold <= 0)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "PRICE_CHANGE_PCT 규칙에는 양수 threshold(%)가 필요합니다");
    }
    if (req.condition() == AlertCondition.NEW_TRADE) {
      threshold = null; // NEW_TRADE ignores threshold
    }

    AlertRule saved =
        alertRules.save(
            new AlertRule(userId, w.getId(), req.condition(), threshold, Instant.now()));
    return AlertRuleResponse.from(saved);
  }

  public void delete(Long userId, Long id) {
    AlertRule rule =
        alertRules
            .findById(id)
            .filter(r -> r.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "없는 규칙입니다"));
    alertRules.delete(rule);
  }
}
