package com.siseradar.web.dto;

/**
 * 동일 단지 변동률 — 두 시점에 모두 거래된 같은 건물+평형대만 골라 단위면적가의 % 변동.
 *
 * @param matchedComplexes 매칭된 (건물+평형대) 셀 수 (표본)
 * @param sameStoreAvgChangePct 셀별 % 변동의 평균 (구성 통제된 "진짜 추세")
 * @param sameStoreMedianChangePct 셀별 % 변동의 중위
 * @param naiveChangePct 같은 두 달 지역 전체 평균 단위면적가의 단순 변동 (대조용, null 가능)
 */
public record ComplexChangeResponse(
    String fromYm,
    String toYm,
    int matchedComplexes,
    Double sameStoreAvgChangePct,
    Double sameStoreMedianChangePct,
    Double naiveChangePct) {}
