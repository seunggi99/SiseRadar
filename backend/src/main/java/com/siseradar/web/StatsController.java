package com.siseradar.web;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import com.siseradar.web.dto.ComplexChangeResponse;
import com.siseradar.web.dto.ComplexRankResponse;
import com.siseradar.web.dto.MonthlyStatsResponse;
import com.siseradar.web.dto.SameStoreChangeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Stats", description = "집계 통계")
public class StatsController {

  private final StatsService statsService;

  public StatsController(StatsService statsService) {
    this.statsService = statsService;
  }

  @GetMapping("/monthly")
  @Operation(summary = "월별 평균/중위/평당가/거래량 + 전월대비 (유형별)")
  public List<MonthlyStatsResponse> monthly(
      @RequestParam String lawdCd,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return statsService.monthly(lawdCd, propertyType, tradeType, from, to);
  }

  @GetMapping("/complexes")
  @Operation(summary = "지역/월별 단지 랭킹 (대표금액 기준, 유형별)")
  public List<ComplexRankResponse> complexes(
      @RequestParam String lawdCd,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String ym) {
    return statsService.complexes(lawdCd, propertyType, tradeType, ym);
  }

  @GetMapping("/complex-change")
  @Operation(summary = "동일 단지(건물+평형대) 변동률 — 임의 두 시점 (레거시)")
  public ComplexChangeResponse complexChange(
      @RequestParam String lawdCd,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return statsService.complexChange(lawdCd, propertyType, tradeType, from, to);
  }

  @GetMapping("/same-store-change")
  @Operation(
      summary = "동일단지 변동률 (최근 12개월 vs 직전 12개월 고정) — 지도·대시보드·AI 공유 단일 지표")
  public SameStoreChangeResponse sameStoreChange(
      @RequestParam String lawdCd,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType) {
    return statsService.sameStoreChange12(lawdCd, propertyType, tradeType);
  }
}
