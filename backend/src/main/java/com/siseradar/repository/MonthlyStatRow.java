package com.siseradar.repository;

/** Projection for the monthly GROUP BY aggregation. Amounts are the primary amount (만원). */
public interface MonthlyStatRow {
  String getYm();

  long getCnt();

  double getAvgAmount();

  double getMedianAmount();

  double getAvgPricePerPyeong();

  /** Average 월세 (만원) — null for SALE. */
  Double getAvgMonthlyRent();
}
