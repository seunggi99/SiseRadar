package com.siseradar.repository;

/**
 * Projection for same-complex change: one (building, area-band) cell traded in BOTH months,
 * with its average 단위면적가 (만원/㎡) in each.
 */
public interface ComplexChangeRow {
  String getBuildingName();

  String getBand();

  double getFromAvg();

  double getToAvg();
}
