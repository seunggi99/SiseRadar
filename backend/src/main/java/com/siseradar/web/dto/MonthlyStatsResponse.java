package com.siseradar.web.dto;

/**
 * One month of aggregates. Amounts are 만원, {@code avgPricePerPyeong} is 만원/평.
 * {@code momChangePct} is the month-over-month change in average price (null for the first month).
 */
public record MonthlyStatsResponse(
    String ym,
    long count,
    long avgAmount,
    long medianAmount,
    long avgPricePerPyeong,
    Double momChangePct) {}
