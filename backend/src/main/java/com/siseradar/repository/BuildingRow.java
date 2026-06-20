package com.siseradar.repository;

/** A distinct building (단지명 + 대표 동) for a (region, propertyType) — drives geocode warming. */
public interface BuildingRow {
  String getBuildingName();

  String getUmdNm();
}
