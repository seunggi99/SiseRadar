import type { TradeType } from '../api/types';

/** teal 순차(낮음→높음). 상승/하락(빨강·파랑)과 절대 안 섞음. */
export const TEAL_SHADES = ['#CDEFEA', '#8FD9CE', '#39C9B9', '#0E9E8F', '#0B5F56'];

/**
 * 고정 ₩/㎡(만원/㎡) 절대 구간 — 마커와 버블이 공유해 "어느 줌·지역이든 같은 색=같은 가격대".
 * 거래유형별 단위가 다름(매매=거래가, 전월세=보증금)이라 스케일도 거래유형별.
 */
const THRESHOLDS: Record<TradeType, number[]> = {
  SALE: [1500, 2200, 3000, 4000], // 만원/㎡
  RENT: [400, 700, 1000, 1400],
};

/** 단위면적가(만원/㎡) → teal 색. */
export function colorForArea(perArea: number, tradeType: TradeType): string {
  const th = THRESHOLDS[tradeType];
  for (let i = 0; i < th.length; i++) if (perArea <= th[i]) return TEAL_SHADES[i];
  return TEAL_SHADES[TEAL_SHADES.length - 1];
}

export function scaleThresholds(tradeType: TradeType): number[] {
  return THRESHOLDS[tradeType];
}

// ── 상승률(1년 변동률) diverging 스케일 ───────────────────────────────────────
// 하락=파랑(#2F6FED 계열) ← 0 중립 → 상승=빨강(#E5484D 계열). teal 수준과 절대 안 섞음.
// 경계 ±10%에서 진한 색으로 클램프(극단값도 같은 진하기).
export const DIVERGING_SHADES = ['#2F6FED', '#9CB8F2', '#D5D8DC', '#F0A6A8', '#E5484D'];
const CHANGE_THRESHOLDS = [-10, -3, 3, 10]; // 경계 4개 → 색 5개 (가운데 중립)

/** 1년 변동률(%) → diverging 색. null(데이터 부족)은 호출부에서 별도 처리. */
export function colorForChange(pct: number): string {
  for (let i = 0; i < CHANGE_THRESHOLDS.length; i++) {
    if (pct <= CHANGE_THRESHOLDS[i]) return DIVERGING_SHADES[i];
  }
  return DIVERGING_SHADES[DIVERGING_SHADES.length - 1];
}

export function changeThresholds(): number[] {
  return CHANGE_THRESHOLDS;
}
