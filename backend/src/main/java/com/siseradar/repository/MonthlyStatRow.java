package com.siseradar.repository;

/** Projection for the monthly GROUP BY aggregation in {@link AptTradeRepository}. */
public interface MonthlyStatRow {
  String getYm();

  long getCnt();

  double getAvgAmount();

  double getMedianAmount();

  double getAvgPricePerPyeong();
}
