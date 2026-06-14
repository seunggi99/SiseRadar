package com.siseradar.domain;

/** 지오코딩 캐시 상태. FAILED도 캐시해 무한 재시도를 막는다. */
public enum GeocodeStatus {
  PENDING,
  SUCCESS,
  FAILED
}
