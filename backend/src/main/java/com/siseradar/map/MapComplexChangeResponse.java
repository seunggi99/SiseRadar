package com.siseradar.map;

/**
 * 단일 단지의 평당가(전용) 변동률 — 현재 12개월 vs 직전 12개월(같은 건물, 가능하면 같은
 * 평형대). 두 기간 모두 거래가 있어야 계산 가능하며, 한쪽이라도 없으면 {@code hasData=false}로
 * "변동 데이터 부족"을 표시한다 (0%나 오해되는 값을 내지 않는다).
 *
 * @param hasData 두 기간 모두 거래가 있어 변동률 계산이 가능한지
 * @param changePct 단위면적가 % 변동 (상승 +, 하락 −) — hasData=false면 null
 * @param currentCount 현재 기간 거래 건수
 * @param previousCount 직전 기간 거래 건수
 */
public record MapComplexChangeResponse(
    boolean hasData,
    Double changePct,
    long currentCount,
    long previousCount,
    String currentFrom,
    String currentTo,
    String previousFrom,
    String previousTo) {}
