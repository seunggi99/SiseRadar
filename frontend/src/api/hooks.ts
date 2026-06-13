import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './client';
import type { AlertCondition, WatchType } from './types';

// ── public ──
export function useMonthlyStats(lawdCd: string) {
  return useQuery({
    queryKey: ['monthly', lawdCd],
    queryFn: () => api.monthlyStats(lawdCd),
  });
}

export function useComplexRanking(lawdCd: string, ym?: string) {
  return useQuery({
    queryKey: ['complexes', lawdCd, ym ?? 'latest'],
    queryFn: () => api.complexRanking(lawdCd, ym),
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
