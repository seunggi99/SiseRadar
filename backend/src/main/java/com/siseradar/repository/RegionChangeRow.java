package com.siseradar.repository;

/** Per-region 동일단지 평균 1년 변동률 (%) + matched same-store 건물 수 — for the 상승률 색 모드. */
public interface RegionChangeRow {
  String getLawdCd();

  Double getChangePct();

  long getMatched();
}
