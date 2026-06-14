package com.siseradar.repository;

/** Per-region aggregate for the low-zoom map (전체 거래 기준 전용 단위면적가 만원/㎡). */
public interface MapRegionStatRow {
  String getLawdCd();

  long getCnt();

  double getAvgPricePerArea();

  double getMedianPricePerArea();
}
