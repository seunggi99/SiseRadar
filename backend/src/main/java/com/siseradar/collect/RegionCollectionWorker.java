package com.siseradar.collect;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs an on-demand backfill for one region in the background. Separate bean so {@code @Async}
 * goes through the Spring proxy. Tracks which regions are currently collecting.
 */
@Component
public class RegionCollectionWorker {

  private static final Logger log = LoggerFactory.getLogger(RegionCollectionWorker.class);

  private final TradeCollectionService collection;
  private final Set<String> inProgress = ConcurrentHashMap.newKeySet();

  public RegionCollectionWorker(TradeCollectionService collection) {
    this.collection = collection;
  }

  public boolean isCollecting(String lawdCd) {
    return inProgress.contains(lawdCd);
  }

  @Async
  public void backfill(String lawdCd, int months) {
    inProgress.add(lawdCd);
    try {
      for (String ym : CollectionScheduler.recentMonths(months)) {
        for (RtmsOperations.TypePair pair : RtmsOperations.ENABLED) {
          try {
            collection.collect(lawdCd, ym, pair.propertyType(), pair.tradeType());
          } catch (RuntimeException e) {
            log.warn(
                "On-demand collect failed {} {} {}/{}: {}",
                lawdCd, ym, pair.propertyType(), pair.tradeType(), e.getMessage());
          }
        }
      }
      log.info("On-demand backfill done: {} ({} months × {} types)", lawdCd, months, RtmsOperations.ENABLED.size());
    } finally {
      inProgress.remove(lawdCd);
    }
  }
}
