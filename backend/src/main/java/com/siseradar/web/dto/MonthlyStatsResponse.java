package com.siseradar.web.dto;

/**
 * One month of aggregates. {@code avgAmount}/{@code medianAmount} are the primary amount (만원:
 * 매매 거래가 / 전월세 보증금); {@code avgPricePerPyeong} is 만원/평; {@code avgMonthlyRent} is 만원
 * (null for SALE). {@code momChangePct} is the month-over-month change in the primary amount.
 */
public record MonthlyStatsResponse(
    String ym,
    long count,
    long avgAmount,
    long medianAmount,
    long avgPricePerPyeong,
    Long avgMonthlyRent,
    Double momChangePct) {}
