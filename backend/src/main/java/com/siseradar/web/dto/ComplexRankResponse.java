package com.siseradar.web.dto;

/**
 * One complex's ranking row for a region+month. Amounts are 만원; {@code avgPricePerPyeong} is 만원/평.
 */
public record ComplexRankResponse(
    int rank,
    String aptName,
    long count,
    long avgAmount,
    long maxAmount,
    long avgPricePerPyeong) {}
