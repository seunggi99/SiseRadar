package com.siseradar.alert;

import com.siseradar.domain.AlertRule;
import com.siseradar.domain.Notification;
import com.siseradar.domain.PropertyType;
import com.siseradar.domain.RealEstateTransaction;
import com.siseradar.domain.TradeType;
import com.siseradar.domain.WatchType;
import com.siseradar.domain.Watchlist;
import com.siseradar.repository.AlertRuleRepository;
import com.siseradar.repository.NotificationRepository;
import com.siseradar.repository.RealEstateTransactionRepository;
import com.siseradar.repository.WatchlistRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * After a collection run inserts new transactions for a region+month+type, raises in-app
 * notifications for users watching that region/complex whose alert rules fire. Only runs when there
 * were new inserts, so re-collecting an unchanged month doesn't re-notify. Price-change uses the
 * primary amount (매매 거래가 / 전월세 보증금) scoped to the collected type.
 */
@Service
public class AlertEvaluationService {

  private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

  private final WatchlistRepository watchlists;
  private final AlertRuleRepository alertRules;
  private final NotificationRepository notifications;
  private final RealEstateTransactionRepository trades;

  public AlertEvaluationService(
      WatchlistRepository watchlists,
      AlertRuleRepository alertRules,
      NotificationRepository notifications,
      RealEstateTransactionRepository trades) {
    this.watchlists = watchlists;
    this.alertRules = alertRules;
    this.notifications = notifications;
    this.trades = trades;
  }

  public void evaluate(
      String lawdCd,
      String ym,
      PropertyType propertyType,
      TradeType tradeType,
      List<RealEstateTransaction> inserted) {
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
            case PRICE_CHANGE_PCT ->
                priceChangeMessage(w, ym, propertyType, tradeType, rule.getThreshold());
          };
      if (message != null) {
        notifications.save(new Notification(rule.getUserId(), message, Instant.now()));
      }
    }
  }

  private String newTradeMessage(Watchlist w, List<RealEstateTransaction> inserted) {
    if (w.getType() == WatchType.REGION) {
      return "[%s] 새 거래 %d건이 포착됐어요.".formatted(w.getLawdCd(), inserted.size());
    }
    long count =
        inserted.stream().filter(t -> w.getAptName().equals(t.getBuildingName())).count();
    return count > 0 ? "[%s] 새 거래 %d건이 포착됐어요.".formatted(w.getAptName(), count) : null;
  }

  private String priceChangeMessage(
      Watchlist w, String ym, PropertyType pt, TradeType tt, Double threshold) {
    if (threshold == null) {
      return null;
    }
    String prevYm = YearMonth.parse(ym, YM).minusMonths(1).format(YM);
    Double cur;
    Double prev;
    String label;
    if (w.getType() == WatchType.REGION) {
      cur = trades.avgPrimaryByRegionAndYm(w.getLawdCd(), pt, tt, ym);
      prev = trades.avgPrimaryByRegionAndYm(w.getLawdCd(), pt, tt, prevYm);
      label = w.getLawdCd();
    } else {
      cur = trades.avgPrimaryByComplexAndYm(w.getLawdCd(), pt, tt, w.getAptName(), ym);
      prev = trades.avgPrimaryByComplexAndYm(w.getLawdCd(), pt, tt, w.getAptName(), prevYm);
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
