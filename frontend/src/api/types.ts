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
  /** 전용면적 평형대별 분해. */
  bands: AreaBand[];
}

export type AreaBandKey = 'SMALL' | 'MID_SMALL' | 'MID_LARGE' | 'LARGE';
export interface AreaBand {
  band: AreaBandKey;
  count: number;
  avgPricePerArea: number;
  medianPricePerArea: number;
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

export interface MapComplex {
  lawdCd: string;
  buildingName: string;
  lat: number;
  lng: number;
  /** 단위면적가 만원/㎡ (전용 기준). 평당 = ×3.3058. */
  avgPricePerArea: number;
  medianPricePerArea: number;
  count: number;
  /** 1년 평당가 변동률 (%) — 상승률 색 모드용. null이면 데이터 부족. */
  changePct: number | null;
}

/** 단일 단지 평당가(전용) 변동률 — 현재 12개월 vs 직전 12개월. hasData=false면 "변동 데이터 부족". */
export interface MapComplexChange {
  hasData: boolean;
  /** % 변동 (상승 +, 하락 −) — hasData=false면 null. */
  changePct: number | null;
  currentCount: number;
  previousCount: number;
  currentFrom: string;
  currentTo: string;
  previousFrom: string;
  previousTo: string;
}

/** Viewport bounding box (lat/lng) for fetching markers in view. */
export interface Bounds {
  swLat: number;
  swLng: number;
  neLat: number;
  neLng: number;
}

export interface MapRegion {
  lawdCd: string;
  lat: number;
  lng: number;
  /** 전체 거래 기준 단위면적가 만원/㎡ (전용). 평당 = ×3.3058. */
  avgPricePerArea: number;
  medianPricePerArea: number;
  /** 전체 거래량 (대시보드와 동일). */
  count: number;
  /** 동일단지 평균 1년 변동률 (%) — 상승률 색 모드용. null이면 데이터 부족. */
  changePct: number | null;
}

/** AI 시장 요약이 근거한 확정 수치 (대시보드와 동일 출처). */
export interface InsightBasis {
  region: string;
  propertyLabel: string;
  tradeLabel: string;
  metricLabel: string;
  periodFrom: string | null;
  periodTo: string | null;
  months: number;
  avgPerPyeong: number;
  medianPerPyeong: number;
  avgPerSqm: number;
  totalVolume: number;
  /** 동일단지(같은 건물+평형대) 변동률 — 대시보드 '동일 단지 추세' 카드와 동일 값·기간. */
  changeAvgPct: number | null;
  changeMedianPct: number | null;
  changeMatched: number;
  bands: { band: string; count: number }[];
  hasData: boolean;
}

export interface RegionInsight {
  summary: string;
  generatedAt: string;
  /** "ai" = LLM 생성, "fallback" = 템플릿(키 없음·쿼터·일시오류). */
  source: 'ai' | 'fallback';
  basis: InsightBasis;
}

/**
 * 동일단지(같은 건물) 변동률 — 최근 12개월 vs 직전 12개월 고정 윈도. 지도 버블·대시보드 카드·
 * AI 요약이 공유하는 단일 지표. hasData=false면 데이터 부족(24개월 미충족).
 */
export interface SameStoreChange {
  hasData: boolean;
  avgPct: number | null;
  medianPct: number | null;
  matched: number;
  curFrom: string | null;
  curTo: string | null;
  prevFrom: string | null;
  prevTo: string | null;
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
