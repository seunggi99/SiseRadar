package com.siseradar.map;

/** A geocoded complex marker for the map. 단위면적가는 만원/㎡ (전용 기준). */
public record MapComplexResponse(
    String lawdCd,
    String buildingName,
    double lat,
    double lng,
    long avgPricePerArea,
    long medianPricePerArea,
    long count) {}
