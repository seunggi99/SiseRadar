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
}
