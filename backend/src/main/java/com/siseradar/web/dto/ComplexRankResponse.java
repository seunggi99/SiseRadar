package com.siseradar.web.dto;

/**
 * One building's ranking row. {@code aptName} is the building name (kept as "aptName" for frontend
 * back-compat). Amounts are the primary amount (만원); {@code avgMonthlyRent} is null for SALE.
 */
public record ComplexRankResponse(
    int rank,
    String aptName,
    long count,
    long avgAmount,
    long maxAmount,
    long avgPricePerPyeong,
    Long avgMonthlyRent) {}
