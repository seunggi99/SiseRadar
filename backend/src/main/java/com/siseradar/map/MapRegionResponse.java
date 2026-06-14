package com.siseradar.map;

/** A region bubble for the low-zoom map. 단위면적가 만원/㎡ (전용 기준), count = 전체 거래량. */
public record MapRegionResponse(
    String lawdCd,
    double lat,
    double lng,
    long avgPricePerArea,
    long medianPricePerArea,
    long count) {}
