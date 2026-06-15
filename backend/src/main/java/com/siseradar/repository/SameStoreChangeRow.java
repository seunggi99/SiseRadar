package com.siseradar.repository;

/** One region's 동일단지 12v12 변동률 — 평균·중위(%) + 매칭 단지 수. null/0이면 데이터 부족. */
public interface SameStoreChangeRow {
  Double getAvgPct();

  Double getMedianPct();

  long getMatched();
}
