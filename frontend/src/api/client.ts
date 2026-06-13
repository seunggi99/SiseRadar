import type { ComplexRank, MonthlyStats, PageResponse, Trade } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

type Params = Record<string, string | number | undefined | null>;

async function get<T>(path: string, params: Params = {}): Promise<T> {
  const url = new URL(path, BASE_URL);
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  }
  const res = await fetch(url.toString());
  if (!res.ok) {
    throw new Error(`API ${res.status} ${res.statusText} (${path})`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  monthlyStats: (lawdCd: string, from?: string, to?: string) =>
    get<MonthlyStats[]>('/api/stats/monthly', { lawdCd, from, to }),

  complexRanking: (lawdCd: string, ym?: string) =>
    get<ComplexRank[]>('/api/stats/complexes', { lawdCd, ym }),

  trades: (params: {
    lawdCd: string;
    from?: string;
    to?: string;
    areaMin?: number;
    areaMax?: number;
    page?: number;
    size?: number;
  }) => get<PageResponse<Trade>>('/api/trades', params),
};
