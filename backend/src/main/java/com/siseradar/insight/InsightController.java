package com.siseradar.insight;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
@Tag(name = "Insight", description = "AI 시장 요약 (그라운디드 · 무료 Gemini)")
public class InsightController {

  private final InsightService insightService;

  public InsightController(InsightService insightService) {
    this.insightService = insightService;
  }

  @GetMapping("/region")
  @Operation(summary = "선택 지역 AI 시장 요약 (백엔드 계산 수치 기반, 캐시·폴백)")
  public RegionInsightResponse region(
      @RequestParam String lawdCd,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return insightService.regionInsight(lawdCd, propertyType, tradeType, from, to);
  }
}
