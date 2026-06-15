package com.siseradar.insight;

import java.util.List;

/**
 * The exact, backend-computed numbers a summary is grounded on (same source as the dashboard).
 * Shown to the user as "기준 지표" and fed to the LLM — which must not invent any other number.
 */
public record InsightBasis(
    String region,
    String propertyLabel,
    String tradeLabel,
    String metricLabel,
    String periodFrom,
    String periodTo,
    int months,
    long avgPerPyeong,
    long medianPerPyeong,
    long avgPerSqm,
    long totalVolume,
    // 동일단지(같은 건물+평형대) 변동률 — 대시보드 '동일 단지 추세' 카드와 같은 계산·기간.
    Double changeAvgPct,
    Double changeMedianPct,
    int changeMatched,
    List<BandCount> bands,
    boolean hasData) {

  public record BandCount(String band, long count) {}
}
