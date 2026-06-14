package com.siseradar.repository;

/** Projection for per-month, per-area-band aggregation. Price-per-area is 만원/㎡. */
public interface BandStatRow {
  String getYm();

  /** SMALL(≤60) / MID_SMALL(60–85) / MID_LARGE(85–135) / LARGE(>135), 전용면적 기준. */
  String getBand();

  long getCnt();

  double getAvgPricePerArea();

  double getMedianPricePerArea();
}
