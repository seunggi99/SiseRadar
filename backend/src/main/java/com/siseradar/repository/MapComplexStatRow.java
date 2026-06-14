package com.siseradar.repository;

/** Per-building stats for the map (단위면적가 만원/㎡, 전용 기준). umdNm helps geocoding. */
public interface MapComplexStatRow {
  String getBuildingName();

  String getUmdNm();

  long getCnt();

  double getAvgPricePerArea();

  double getMedianPricePerArea();
}
