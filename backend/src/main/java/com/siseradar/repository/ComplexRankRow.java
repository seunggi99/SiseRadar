package com.siseradar.repository;

/** Projection for per-building ranking. Amounts are the primary amount (만원). */
public interface ComplexRankRow {
  String getBuildingName();

  long getCnt();

  double getAvgAmount();

  long getMaxAmount();

  double getAvgPricePerPyeong();

  /** Average 월세 (만원) — null for SALE. */
  Double getAvgMonthlyRent();
}
