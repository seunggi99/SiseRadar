package com.siseradar.repository;

/** A geocoded complex marker (coords from the cache + 전용 단위면적가 stats) within a viewport bbox. */
public interface MapMarkerRow {
  String getLawdCd();

  String getBuildingName();

  double getLat();

  double getLng();

  long getCnt();

  double getAvgPricePerArea();

  double getMedianPricePerArea();

  // current / prior 12-month windows — for the 1년 변동률 color mode (null when no transactions)
  Double getCurAvg();

  Double getPrevAvg();

  long getCurCnt();

  long getPrevCnt();

  /** 이 단지 거래의 최초 거래월(YYYYMM) — 부분수집 지역 커버리지 배너 판정용. */
  String getEarliestYmd();
}
