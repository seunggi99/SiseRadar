package com.siseradar.domain;

/** RTMS 부동산 유형. 1차 수집은 APT만, 스키마는 전체 대응. */
public enum PropertyType {
  APT, // 아파트
  OFFICETEL, // 오피스텔
  ROW_HOUSE, // 연립다세대
  DETACHED, // 단독/다가구
  LAND, // 토지
  COMMERCIAL, // 상업업무용
  INDUSTRIAL, // 공장·창고 등 산업용
  PRESALE_RIGHT // 분양권/입주권 전매
}
