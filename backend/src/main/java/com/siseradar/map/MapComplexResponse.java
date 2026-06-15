package com.siseradar.map;

/**
 * A geocoded complex marker for the map. 단위면적가는 만원/㎡ (전용 기준). {@code changePct} =
 * 1년 평당가 변동률(%) for the 상승률 색 모드 — null이면 "변동 데이터 부족"(두 기간 거래 필요).
 */
public record MapComplexResponse(
    String lawdCd,
    String buildingName,
    double lat,
    double lng,
    long avgPricePerArea,
    long medianPricePerArea,
    long count,
    Double changePct) {}
