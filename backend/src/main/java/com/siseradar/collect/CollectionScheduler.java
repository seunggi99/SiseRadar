package com.siseradar.collect;

import com.siseradar.map.MapService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Re-collects the configured regions for the most recent N months every morning.
 * Recent months keep gaining filings (신고 지연), so they're re-fetched on each run;
 * the collection service makes that idempotent.
 */
@Component
public class CollectionScheduler {

  private static final Logger log = LoggerFactory.getLogger(CollectionScheduler.class);

  private final TradeCollectionService collectionService;
  private final CollectionProperties props;
  private final MapService mapService;

  public CollectionScheduler(
      TradeCollectionService collectionService, CollectionProperties props, MapService mapService) {
    this.collectionService = collectionService;
    this.props = props;
    this.mapService = mapService;
  }

  @Scheduled(cron = "${siseradar.collection.cron:0 0 4 * * *}")
  public void collectRecent() {
    List<String> months = recentMonths(props.recentMonths());
    log.info("Scheduled collection start: regions={} months={}", props.lawdCodes(), months);
    for (String lawdCd : props.lawdCodes()) {
      for (String ym : months) {
        for (RtmsOperations.TypePair pair : RtmsOperations.ENABLED) {
          try {
            collectionService.collect(lawdCd, ym, pair.propertyType(), pair.tradeType());
          } catch (RuntimeException e) {
            log.error(
                "Collection failed for {} {} {}/{}: {}",
                lawdCd, ym, pair.propertyType(), pair.tradeType(), e.getMessage());
          }
        }
      }
    }
    // 수집 직후 지도 버블 캐시를 미리 채워, 다음 첫 방문자도 즉시(무거운 집계 cache-miss 없이).
    mapService.warmRegionsCache();
  }

  /** YYYYMM strings for the current month and the previous (recentMonths - 1) months. */
  static List<String> recentMonths(int recentMonths) {
    int n = Math.max(1, recentMonths);
    YearMonth current = YearMonth.from(LocalDate.now());
    List<String> months = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      months.add(current.minusMonths(i).toString().replace("-", ""));
    }
    return months;
  }
}
