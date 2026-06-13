package com.siseradar.collect;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual collection trigger — handy for seeding/backfill and for waking a sleeping free-tier
 * host via an external cron. TODO(Phase 3): protect this once auth lands.
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Internal", description = "수집 트리거 (운영용)")
public class InternalCollectionController {

  private final TradeCollectionService collectionService;

  public InternalCollectionController(TradeCollectionService collectionService) {
    this.collectionService = collectionService;
  }

  @PostMapping("/collect")
  @Operation(summary = "한 지역의 한 달(또는 최근 N개월) 거래를 수집한다")
  public List<TradeCollectionService.Result> collect(
      @RequestParam String lawdCd,
      @RequestParam(required = false) String dealYmd,
      @RequestParam(required = false, defaultValue = "1") int recentMonths) {
    List<String> months =
        dealYmd != null ? List.of(dealYmd) : CollectionScheduler.recentMonths(recentMonths);
    return months.stream().map(ym -> collectionService.collect(lawdCd, ym)).toList();
  }
}
