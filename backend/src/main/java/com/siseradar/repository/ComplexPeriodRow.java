package com.siseradar.repository;

/** One building's 단위면적가(전용) 평균 + 거래 건수 over a period — for 단지 변동률. */
public interface ComplexPeriodRow {
  Double getAvgPricePerArea();

  long getCnt();
}
