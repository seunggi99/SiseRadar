package com.siseradar.collect;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import com.siseradar.map.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
  private final MapService mapService;

  public InternalCollectionController(
      TradeCollectionService collectionService, MapService mapService) {
    this.collectionService = collectionService;
    this.mapService = mapService;
  }

  @PostMapping("/geocode")
  @Operation(summary = "한 지역의 주거 단지를 일괄 지오코딩(캐시 워밍)한다 — 멱등, 스로틀 시 429")
  public ResponseEntity<MapService.WarmResult> geocode(@RequestParam String lawdCd) {
    MapService.WarmResult r = mapService.warmRegion(lawdCd);
    // 스로틀/쿼터로 중단되면 429 → 드라이버가 멈추고 "어디까지" 보고 후 resume.
    return r.throttled()
        ? ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(r)
        : ResponseEntity.ok(r);
  }

  @PostMapping("/collect")
  @Operation(summary = "한 지역의 한 달(또는 최근 N개월) × 활성 유형 거래를 수집한다")
  public List<TradeCollectionService.Result> collect(
      @RequestParam String lawdCd,
      @RequestParam(required = false) String dealYmd,
      @RequestParam(required = false, defaultValue = "1") int recentMonths,
      @RequestParam(required = false) String types,
      @RequestParam(required = false) String trades) {
    List<String> months =
        dealYmd != null ? List.of(dealYmd) : CollectionScheduler.recentMonths(recentMonths);
    // types=APT,OFFICETEL,... / trades=SALE,RENT 로 수집 범위를 좁힐 수 있다(미지정=전체). data.go.kr
    // 일일 쿼터가 유형·거래별로 따로 소진되므로(예: 전월세 API만 소진), 남은 것만 분리 수집하기 위함.
    List<RtmsOperations.TypePair> pairs = RtmsOperations.ENABLED;
    if (types != null && !types.isBlank()) {
      Set<PropertyType> wanted =
          Arrays.stream(types.split(","))
              .map(String::trim)
              .filter(s -> !s.isBlank())
              .map(s -> PropertyType.valueOf(s.toUpperCase()))
              .collect(Collectors.toSet());
      pairs = pairs.stream().filter(p -> wanted.contains(p.propertyType())).toList();
    }
    if (trades != null && !trades.isBlank()) {
      Set<TradeType> wantedTrades =
          Arrays.stream(trades.split(","))
              .map(String::trim)
              .filter(s -> !s.isBlank())
              .map(s -> TradeType.valueOf(s.toUpperCase()))
              .collect(Collectors.toSet());
      pairs = pairs.stream().filter(p -> wantedTrades.contains(p.tradeType())).toList();
    }
    List<TradeCollectionService.Result> results = new ArrayList<>();
    for (String ym : months) {
      for (RtmsOperations.TypePair pair : pairs) {
        results.add(collectionService.collect(lawdCd, ym, pair.propertyType(), pair.tradeType()));
      }
    }
    return results;
  }
}
