package com.siseradar.web.dto;

/**
 * 동일단지(같은 건물) 변동률 — 최근 12개월 vs 직전 12개월 고정 윈도(전국 데이터의 최신월 기준).
 * 지도 버블·대시보드 카드·AI 요약이 모두 이 단일 계산을 쓴다. 24개월(12×2)이 안 채워져 두 윈도에
 * 모두 거래된 단지가 없으면 {@code hasData=false}(데이터 부족) — 억지 0% 금지.
 *
 * @param avgPct 동일단지 % 변동의 평균 — hasData=false면 null
 * @param medianPct 동일단지 % 변동의 중위 — null 가능
 * @param matched 두 윈도에 모두 거래된 동일 단지 수(표본)
 */
public record SameStoreChangeResponse(
    boolean hasData,
    Double avgPct,
    Double medianPct,
    long matched,
    String curFrom,
    String curTo,
    String prevFrom,
    String prevTo) {}
