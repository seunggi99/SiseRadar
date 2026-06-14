package com.siseradar.repository;

/** Region centroid = average of its cached (SUCCESS) complex coordinates. */
public interface RegionCentroidRow {
  String getLawdCd();

  double getLat();

  double getLng();
}
