package com.siseradar.repository;

/** A distinct building (단지명 + 대표 동·지번) for a (region, propertyType) — drives geocode warming. */
public interface BuildingRow {
  String getBuildingName();

  String getUmdNm();

  /** 대표 지번(번지) — 단지명 검색 실패 시 "법정동+지번" 주소 폴백에 사용. */
  String getJibun();
}
