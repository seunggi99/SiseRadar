package com.siseradar.repository;

/**
 * Projection for the monthly GROUP BY aggregation. Amounts are the primary amount (만원).
 * Price-per-area is 만원/㎡ on the 전용면적 (for APT/오피스텔/연립; 단독·상업·토지는 각 유형 면적 기준).
 */
public interface MonthlyStatRow {
  String getYm();

  long getCnt();

  /** 평균 거래가/보증금 (만원) — 참고용(거래 구성에 휘둘림). */
  double getAvgAmount();

  double getMedianAmount();

  /** 평균 단위면적가 (만원/㎡). */
  double getAvgPricePerArea();

  /** 중위 단위면적가 (만원/㎡). */
  double getMedianPricePerArea();

  /** 평균 월세 (만원) — null for SALE. */
  Double getAvgMonthlyRent();
}
