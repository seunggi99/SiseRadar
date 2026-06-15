package com.siseradar.map;

/**
 * A region bubble for the low-zoom map. 단위면적가 만원/㎡ (전용 기준), count = 전체 거래량.
 * {@code changePct} = 동일단지 평균 1년 변동률(%) for the 상승률 색 모드 — null이면 데이터 부족.
 */
public record MapRegionResponse(
    String lawdCd,
    double lat,
    double lng,
    long avgPricePerArea,
    long medianPricePerArea,
    long count,
    Double changePct) {}
