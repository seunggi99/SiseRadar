import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './client';
import type { AlertCondition, Bounds, PropertyType, TradeType, WatchType } from './types';

// ── public ──
export function useMonthlyStats(lawdCd: string, propertyType: PropertyType, tradeType: TradeType) {
  return useQuery({
    queryKey: ['monthly', lawdCd, propertyType, tradeType],
    queryFn: () => api.monthlyStats(lawdCd, propertyType, tradeType),
  });
}

export function useComplexRanking(
  lawdCd: string,
  propertyType: PropertyType,
  tradeType: TradeType,
  ym?: string,
) {
  return useQuery({
    queryKey: ['complexes', lawdCd, propertyType, tradeType, ym ?? 'latest'],
    queryFn: () => api.complexRanking(lawdCd, propertyType, tradeType, ym),
  });
}

/** Same-complex (building + area-band) change over a period — composition-controlled trend. */
export function useComplexChange(
  lawdCd: string,
  propertyType: PropertyType,
  tradeType: TradeType,
  from: string | undefined,
  to: string | undefined,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ['complexChange', lawdCd, propertyType, tradeType, from, to],
    queryFn: () => api.complexChange(lawdCd, propertyType, tradeType, from, to),
    enabled,
  });
}

/** All transactions for one complex (across collected months) — for the detail view. */
export function useComplexTrades(
  lawdCd: string,
  aptName: string | null,
  propertyType: PropertyType,
  tradeType: TradeType,
) {
  return useQuery({
    queryKey: ['complexTrades', lawdCd, aptName, propertyType, tradeType],
    queryFn: () => api.trades({ lawdCd, propertyType, tradeType, aptName: aptName!, size: 200 }),
    enabled: aptName !== null,
  });
}

/** 선택 지역 AI 시장 요약 — 백엔드 캐시(하루 TTL)라 지역/유형당 LLM 호출은 드물게. */
export function useRegionInsight(
  lawdCd: string,
  propertyType: PropertyType,
  tradeType: TradeType,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ['insight', lawdCd, propertyType, tradeType],
    queryFn: () => api.insights.region(lawdCd, propertyType, tradeType),
    enabled,
    staleTime: 5 * 60 * 1000,
  });
}

// ── map ──
export function useMapComplexes(
  lawdCd: string,
  propertyType: PropertyType,
  tradeType: TradeType,
  from: string | undefined,
  to: string | undefined,
  band: string | undefined,
) {
  return useQuery({
    queryKey: ['mapComplexes', lawdCd, propertyType, tradeType, from, to, band],
    queryFn: () => api.map.complexes(lawdCd, propertyType, tradeType, from, to, band),
    // poll a bounded number of times while background geocoding fills markers, then stop
    // (Kakao 무료 쿼터 보호). 지역/필터 변경 시 queryKey가 바뀌어 카운트가 리셋됨.
    refetchInterval: (query) => (query.state.dataUpdateCount < 8 ? 8000 : false),
  });
}

/** High-zoom markers for the current viewport bbox. Lazy geocoding fills in over a few polls. */
export function useMapComplexesInBounds(
  bounds: Bounds | null,
  propertyType: PropertyType,
  tradeType: TradeType,
  from: string | undefined,
  to: string | undefined,
  band: string | undefined,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ['mapComplexesBbox', bounds, propertyType, tradeType, from, to, band],
    queryFn: () => api.map.complexesInBounds(bounds!, propertyType, tradeType, from, to, band),
    enabled: enabled && bounds !== null,
    // bounded polling while background geocoding fills markers, then stop (Kakao quota 보호)
    refetchInterval: (query) => (query.state.dataUpdateCount < 6 ? 8000 : false),
  });
}

/**
 * Low-zoom region bubbles — 전체 거래 집계 + 시군구 centroid 캐시. centroid가 처음엔 큐잉돼
 * 비어 올 수 있어 몇 번만 폴링(1회성 지오코딩 후 채워짐).
 */
export function useMapRegions(
  propertyType: PropertyType,
  tradeType: TradeType,
  from: string | undefined,
  to: string | undefined,
  band: string | undefined,
) {
  return useQuery({
    queryKey: ['mapRegions', propertyType, tradeType, from, to, band],
    queryFn: () => api.map.regions(propertyType, tradeType, from, to, band),
    refetchInterval: (query) => (query.state.dataUpdateCount < 6 ? 4000 : false),
  });
}

// ── region on-demand collection ──
export function useRegionStatus(lawdCd: string, enabled: boolean) {
  return useQuery({
    queryKey: ['regionStatus', lawdCd],
    queryFn: () => api.regions.status(lawdCd),
    enabled,
    // poll while a backfill is running
    refetchInterval: (query) =>
      query.state.data?.state === 'COLLECTING' ? 4000 : false,
  });
}

export function useCollectRegion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (lawdCd: string) => api.regions.collect(lawdCd),
    onSuccess: (_data, lawdCd) =>
      qc.invalidateQueries({ queryKey: ['regionStatus', lawdCd] }),
  });
}

// ── watchlist ──
export function useWatchlist(enabled: boolean) {
  return useQuery({
    queryKey: ['me', 'watchlist'],
    queryFn: () => api.watchlist.list(),
    enabled,
  });
}

export function useAddWatchlist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { type: WatchType; lawdCd: string; aptName?: string }) =>
      api.watchlist.add(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['me', 'watchlist'] }),
  });
}

export function useRemoveWatchlist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.watchlist.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['me', 'watchlist'] });
      qc.invalidateQueries({ queryKey: ['me', 'alerts'] });
    },
  });
}

// ── alert rules ──
export function useAlerts(enabled: boolean) {
  return useQuery({
    queryKey: ['me', 'alerts'],
    queryFn: () => api.alerts.list(),
    enabled,
  });
}

export function useAddAlert() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { watchlistId: number; condition: AlertCondition; threshold?: number }) =>
      api.alerts.add(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['me', 'alerts'] }),
  });
}

export function useRemoveAlert() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.alerts.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['me', 'alerts'] }),
  });
}

// ── notifications ──
export function useNotifications(enabled: boolean) {
  return useQuery({
    queryKey: ['me', 'notifications'],
    queryFn: () => api.notifications.list(),
    enabled,
  });
}

export function useUnreadCount(enabled: boolean) {
  return useQuery({
    queryKey: ['me', 'unread'],
    queryFn: () => api.notifications.unreadCount(),
    enabled,
    refetchInterval: enabled ? 30_000 : false,
  });
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.notifications.markRead(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['me', 'notifications'] });
      qc.invalidateQueries({ queryKey: ['me', 'unread'] });
    },
  });
}
