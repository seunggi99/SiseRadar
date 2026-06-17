package com.siseradar.collect;

import com.siseradar.domain.PropertyType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
  @Operation(summary = "한 지역의 한 달(또는 최근 N개월) × 활성 유형 거래를 수집한다")
  public List<TradeCollectionService.Result> collect(
      @RequestParam String lawdCd,
      @RequestParam(required = false) String dealYmd,
      @RequestParam(required = false, defaultValue = "1") int recentMonths,
      @RequestParam(required = false) String types) {
    List<String> months =
        dealYmd != null ? List.of(dealYmd) : CollectionScheduler.recentMonths(recentMonths);
    // types=APT,OFFICETEL,... 로 수집할 유형을 좁힐 수 있다(미지정=전유형). data.go.kr 저한도 API
    // (토지·분양권 등) 일일 쿼터 소진 시 주거 유형만 분리 수집하기 위함.
    List<RtmsOperations.TypePair> pairs = RtmsOperations.ENABLED;
    if (types != null && !types.isBlank()) {
      Set<PropertyType> wanted =
          Arrays.stream(types.split(","))
              .map(String::trim)
              .filter(s -> !s.isBlank())
              .map(s -> PropertyType.valueOf(s.toUpperCase()))
              .collect(Collectors.toSet());
      pairs = RtmsOperations.ENABLED.stream().filter(p -> wanted.contains(p.propertyType())).toList();
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
