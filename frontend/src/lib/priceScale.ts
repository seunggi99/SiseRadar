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
