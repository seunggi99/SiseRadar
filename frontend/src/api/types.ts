// Mirrors the backend DTOs. Amounts are in 만원; areaPyeong/pricePerPyeong are 평 / 만원·평.

export interface MonthlyStats {
  ym: string;
  count: number;
  avgAmount: number;
  medianAmount: number;
  avgPricePerPyeong: number;
  momChangePct: number | null;
}

export interface Trade {
  id: number;
  aptName: string;
  umdNm: string | null;
  jibun: string | null;
  area: number;
  areaPyeong: number;
  floor: number;
  buildYear: number | null;
  dealAmount: number;
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
