package com.siseradar.alert;

import com.siseradar.domain.AlertRule;
import com.siseradar.domain.AptTrade;
import com.siseradar.domain.Notification;
import com.siseradar.domain.WatchType;
import com.siseradar.domain.Watchlist;
import com.siseradar.repository.AlertRuleRepository;
import com.siseradar.repository.AptTradeRepository;
import com.siseradar.repository.NotificationRepository;
import com.siseradar.repository.WatchlistRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * After a collection run inserts new trades for a region+month, raises in-app notifications for
 * users watching that region/complex whose alert rules fire. Only runs when there were new
 * inserts, so re-collecting an unchanged month doesn't re-notify.
 */
@Service
public class AlertEvaluationService {

  private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

  private final WatchlistRepository watchlists;
  private final AlertRuleRepository alertRules;
  private final NotificationRepository notifications;
  private final AptTradeRepository trades;

  public AlertEvaluationService(
      WatchlistRepository watchlists,
      AlertRuleRepository alertRules,
      NotificationRepository notifications,
      AptTradeRepository trades) {
    this.watchlists = watchlists;
    this.alertRules = alertRules;
    this.notifications = notifications;
    this.trades = trades;
  }

  public void evaluate(String lawdCd, String ym, List<AptTrade> inserted) {
    if (inserted.isEmpty()) {
      return;
    }
    List<Watchlist> watching = watchlists.findByLawdCd(lawdCd);
    if (watching.isEmpty()) {
      return;
    }
    Map<Long, Watchlist> byId = watching.stream().collect(Collectors.toMap(Watchlist::getId, w -> w));
    List<AlertRule> rules = alertRules.findByWatchlistIdIn(byId.keySet());

    for (AlertRule rule : rules) {
      Watchlist w = byId.get(rule.getWatchlistId());
      if (w == null) {
        continue;
      }
      String message =
          switch (rule.getCondition()) {
            case NEW_TRADE -> newTradeMessage(w, inserted);
            case PRICE_CHANGE_PCT -> priceChangeMessage(w, ym, rule.getThreshold());
          };
      if (message != null) {
        notifications.save(new Notification(rule.getUserId(), message, Instant.now()));
      }
    }
  }

  private String newTradeMessage(Watchlist w, List<AptTrade> inserted) {
    if (w.getType() == WatchType.REGION) {
      return "[%s] 새 거래 %d건이 포착됐어요.".formatted(w.getLawdCd(), inserted.size());
    }
    long count = inserted.stream().filter(t -> t.getAptName().equals(w.getAptName())).count();
    return count > 0 ? "[%s] 새 거래 %d건이 포착됐어요.".formatted(w.getAptName(), count) : null;
  }

  private String priceChangeMessage(Watchlist w, String ym, Double threshold) {
    if (threshold == null) {
      return null;
    }
    String prevYm = YearMonth.parse(ym, YM).minusMonths(1).format(YM);
    Double cur;
    Double prev;
    String label;
    if (w.getType() == WatchType.REGION) {
      cur = trades.avgAmountByRegionAndYm(w.getLawdCd(), ym);
      prev = trades.avgAmountByRegionAndYm(w.getLawdCd(), prevYm);
      label = w.getLawdCd();
    } else {
      cur = trades.avgAmountByComplexAndYm(w.getLawdCd(), w.getAptName(), ym);
      prev = trades.avgAmountByComplexAndYm(w.getLawdCd(), w.getAptName(), prevYm);
      label = w.getAptName();
    }
    if (cur == null || prev == null || prev == 0) {
      return null;
    }
    double pct = (cur - prev) / prev * 100.0;
    if (Math.abs(pct) < threshold) {
      return null;
    }
    double rounded = Math.round(pct * 10.0) / 10.0;
    return "[%s] 평균가가 전월 대비 %s%.1f%% 움직였어요 (임계치 %.1f%%)."
        .formatted(label, pct > 0 ? "+" : "", rounded, threshold);
  }
}
