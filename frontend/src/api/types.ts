// Mirrors the backend DTOs. Amounts are in 만원; areaPyeong/pricePerPyeong are 평 / 만원·평.

export type PropertyType =
  | 'APT'
  | 'OFFICETEL'
  | 'ROW_HOUSE'
  | 'DETACHED'
  | 'LAND'
  | 'COMMERCIAL'
  | 'INDUSTRIAL'
  | 'PRESALE_RIGHT';
export type TradeType = 'SALE' | 'RENT';

export interface MonthlyStats {
  ym: string;
  count: number;
  /** Primary amount (만원): 매매 거래가 / 전월세 보증금. 거래 구성에 휘둘리는 참고용. */
  avgAmount: number;
  medianAmount: number;
  /** 단위면적가 평균/중위 — 만원/㎡ (전용면적 기준). 평당 = ×3.3058. */
  avgPricePerArea: number;
  medianPricePerArea: number;
  /** 평균 월세 (만원) — null for SALE. */
  avgMonthlyRent: number | null;
  /** 전월 대비 중위 단위면적가 변화율 (%). */
  momChangePct: number | null;
}

export interface Trade {
  id: number;
  propertyType: PropertyType;
  tradeType: TradeType;
  aptName: string;
  umdNm: string | null;
  jibun: string | null;
  area: number | null;
  areaPyeong: number | null;
  floor: number | null;
  buildYear: number | null;
  /** SALE 거래가 (만원), null for RENT. */
  dealAmount: number | null;
  /** RENT 보증금 (만원), null for SALE. */
  deposit: number | null;
  /** RENT 월세 (만원, 0=전세), null for SALE. */
  monthlyRent: number | null;
  dealDate: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ComplexRank {
  rank: number;
  aptName: string;
  count: number;
  avgAmount: number;
  maxAmount: number;
  avgPricePerPyeong: number;
  avgMonthlyRent: number | null;
}

// ── auth / watchlist / alerts / notifications ──────────────────────────────
export type WatchType = 'REGION' | 'COMPLEX';
export type AlertCondition = 'NEW_TRADE' | 'PRICE_CHANGE_PCT';

export interface AuthResponse {
  token: string;
  email: string;
}

export interface WatchlistItem {
  id: number;
  type: WatchType;
  lawdCd: string;
  aptName: string | null;
  label: string;
  createdAt: string;
}

export interface AlertRule {
  id: number;
  watchlistId: number;
  condition: AlertCondition;
  threshold: number | null;
  createdAt: string;
}

export interface NotificationItem {
  id: number;
  message: string;
  read: boolean;
  createdAt: string;
}

export type RegionCollectState = 'NONE' | 'COLLECTING' | 'DONE';
export interface RegionStatus {
  state: RegionCollectState;
  months: number;
}

export interface ResolvedRegion {
  lawdCd: string;
  sido: string;
  sigungu: string;
}
