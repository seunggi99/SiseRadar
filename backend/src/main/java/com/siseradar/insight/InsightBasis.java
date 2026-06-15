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
    Double change1yPct,
    List<BandCount> bands,
    boolean hasData) {

  public record BandCount(String band, long count) {}
}
