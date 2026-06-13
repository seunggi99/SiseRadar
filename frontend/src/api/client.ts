import type {
  AlertCondition,
  AlertRule,
  AuthResponse,
  ComplexRank,
  MonthlyStats,
  NotificationItem,
  PageResponse,
  Trade,
  WatchlistItem,
  WatchType,
} from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const TOKEN_KEY = 'siseradar-token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}
export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

// Registered by AuthProvider — called when a token is rejected so the app can log out.
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: (() => void) | null) {
  onUnauthorized = fn;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

type Params = Record<string, string | number | undefined | null>;

async function request<T>(
  method: string,
  path: string,
  opts: { params?: Params; body?: unknown } = {},
): Promise<T> {
  const url = new URL(path, BASE_URL);
  for (const [key, value] of Object.entries(opts.params ?? {})) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  }

  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  let body: string | undefined;
  if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(opts.body);
  }

  const res = await fetch(url.toString(), { method, headers, body });

  // A rejected token on a protected call means our session is stale — log out.
  if ((res.status === 401 || res.status === 403) && !path.startsWith('/api/auth')) {
    onUnauthorized?.();
  }
  if (!res.ok) {
    let message = res.statusText;
    try {
      const data = await res.json();
      if (data?.message) message = data.message;
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  // ── public ──
  monthlyStats: (lawdCd: string, from?: string, to?: string) =>
    request<MonthlyStats[]>('GET', '/api/stats/monthly', { params: { lawdCd, from, to } }),

  complexRanking: (lawdCd: string, ym?: string) =>
    request<ComplexRank[]>('GET', '/api/stats/complexes', { params: { lawdCd, ym } }),

  trades: (params: {
    lawdCd: string;
    aptName?: string;
    from?: string;
    to?: string;
    areaMin?: number;
    areaMax?: number;
    page?: number;
    size?: number;
  }) => request<PageResponse<Trade>>('GET', '/api/trades', { params }),

  // ── auth ──
  signup: (email: string, password: string) =>
    request<AuthResponse>('POST', '/api/auth/signup', { body: { email, password } }),
  login: (email: string, password: string) =>
    request<AuthResponse>('POST', '/api/auth/login', { body: { email, password } }),

  // ── watchlist ──
  watchlist: {
    list: () => request<WatchlistItem[]>('GET', '/api/watchlist'),
    add: (body: { type: WatchType; lawdCd: string; aptName?: string }) =>
      request<WatchlistItem>('POST', '/api/watchlist', { body }),
    remove: (id: number) => request<void>('DELETE', `/api/watchlist/${id}`),
  },

  // ── alert rules ──
  alerts: {
    list: () => request<AlertRule[]>('GET', '/api/alerts'),
    add: (body: { watchlistId: number; condition: AlertCondition; threshold?: number }) =>
      request<AlertRule>('POST', '/api/alerts', { body }),
    remove: (id: number) => request<void>('DELETE', `/api/alerts/${id}`),
  },

  // ── notifications ──
  notifications: {
    list: () => request<NotificationItem[]>('GET', '/api/notifications'),
    unreadCount: () =>
      request<{ count: number }>('GET', '/api/notifications/unread-count'),
    markRead: (id: number) => request<void>('PATCH', `/api/notifications/${id}/read`),
  },
};
