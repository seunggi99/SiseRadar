package com.siseradar.insight;

import java.time.Instant;

/**
 * AI 시장 요약 응답. {@code source}: "ai"(LLM 생성) | "fallback"(템플릿) — 실시간·투자조언이 아님을
 * 프론트가 라벨로 드러낸다. {@code basis} = 요약이 근거한 확정 수치(대시보드와 동일 출처).
 */
public record RegionInsightResponse(
    String summary, Instant generatedAt, String source, InsightBasis basis) {}
