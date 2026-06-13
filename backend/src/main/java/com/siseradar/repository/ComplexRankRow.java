package com.siseradar.repository;

/** Projection for the per-complex ranking GROUP BY in {@link AptTradeRepository}. */
public interface ComplexRankRow {
  String getAptName();

  long getCnt();

  double getAvgAmount();

  long getMaxAmount();

  double getAvgPricePerPyeong();
}
