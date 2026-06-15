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
}
