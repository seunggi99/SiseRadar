package com.siseradar.web.dto;

import java.util.List;

/**
 * One month of aggregates, scoped by property/trade type.
 *
 * <ul>
 *   <li>{@code avgAmount}/{@code medianAmount} — 평균/중위 거래가(매매) 또는 보증금(전월세), 만원.
 *       avgAmount는 거래 구성에 휘둘리는 <b>참고용</b> 지표.
 *   <li>{@code avgPricePerArea}/{@code medianPricePerArea} — 단위면적가, <b>만원/㎡</b>(전용면적 기준).
 *       화면 평당가(전용) = ×3.3058.
 *   <li>{@code avgMonthlyRent} — 평균 월세(만원), null for SALE.
 *   <li>{@code momChangePct} — 전월 대비 <b>중위 단위면적가</b> 변화율(%).
 *   <li>{@code bands} — 전용면적 평형대별 분해(거래량·단위면적가).
 * </ul>
 */
public record MonthlyStatsResponse(
    String ym,
    long count,
    long avgAmount,
    long medianAmount,
    long avgPricePerArea,
    long medianPricePerArea,
    Long avgMonthlyRent,
    Double momChangePct,
    List<BandStat> bands) {

  /** 평형대 구간 한 칸 (전용면적 기준). 단위면적가는 만원/㎡. */
  public record BandStat(
      String band, long count, long avgPricePerArea, long medianPricePerArea) {}
}
