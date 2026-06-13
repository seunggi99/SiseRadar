import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client';
import {
  useAddAlert,
  useAlerts,
  useRemoveAlert,
  useRemoveWatchlist,
  useWatchlist,
} from '../api/hooks';
import type { AlertRule, WatchlistItem } from '../api/types';
import { Header } from '../components/Header';
import { LoadingState } from '../components/StateViews';
import { regionName } from '../lib/regions';
import { useToast } from '../lib/toast';

export function WatchlistPage() {
  const watchlist = useWatchlist(true);
  const alerts = useAlerts(true);

  return (
    <div className="min-h-screen">
      <Header />
      <main className="mx-auto max-w-3xl px-5 py-6">
        <h1 className="mb-1 text-xl font-medium tracking-tight">관심목록</h1>
        <p className="sr-muted mb-6 text-sm">
          관심 항목마다 알림 규칙을 추가하면 새 거래·가격 변동을 알려드려요.
        </p>

        {watchlist.isLoading ? (
          <LoadingState />
        ) : !watchlist.data?.length ? (
          <div className="sr-card text-center">
            <p className="sr-muted text-sm">
              아직 관심 항목이 없어요.{' '}
              <Link to="/" style={{ color: 'var(--sr-accent)' }}>
                대시보드
              </Link>
              에서 지역이나 단지를 추가해보세요.
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {watchlist.data.map((item) => (
              <WatchlistRow
                key={item.id}
                item={item}
                rules={(alerts.data ?? []).filter((r) => r.watchlistId === item.id)}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}

function WatchlistRow({ item, rules }: { item: WatchlistItem; rules: AlertRule[] }) {
  const { toast } = useToast();
  const removeWatchlist = useRemoveWatchlist();
  const addAlert = useAddAlert();
  const removeAlert = useRemoveAlert();
  const [pct, setPct] = useState('5');

  const title = item.type === 'COMPLEX' ? item.aptName : regionName(item.lawdCd);
  const typeLabel = item.type === 'COMPLEX' ? '단지' : '지역';

  const hasNewTrade = rules.some((r) => r.condition === 'NEW_TRADE');

  function addNewTrade() {
    if (hasNewTrade) return;
    addAlert.mutate(
      { watchlistId: item.id, condition: 'NEW_TRADE' },
      { onSuccess: () => toast('새 거래 알림을 켰어요'), onError: onErr },
    );
  }

  function addPriceChange() {
    const threshold = Number(pct);
    if (!threshold || threshold <= 0) {
      toast('0보다 큰 퍼센트를 입력해 주세요', 'error');
      return;
    }
    addAlert.mutate(
      { watchlistId: item.id, condition: 'PRICE_CHANGE_PCT', threshold },
      { onSuccess: () => toast(`가격 변동 ±${threshold}% 알림을 켰어요`), onError: onErr },
    );
  }

  function onErr(err: unknown) {
    toast(err instanceof ApiError ? err.message : '실패했어요', 'error');
  }

  return (
    <div className="sr-card flex flex-col gap-3">
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-0.5">
          <span className="font-medium">{title}</span>
          <span className="sr-muted text-xs">
            {typeLabel} · {item.lawdCd}
          </span>
        </div>
        <button
          className="sr-muted text-xs hover:text-[var(--sr-up)]"
          onClick={() =>
            removeWatchlist.mutate(item.id, {
              onSuccess: () => toast('관심목록에서 제거됨'),
              onError: onErr,
            })
          }
        >
          제거
        </button>
      </div>

      {/* existing rules */}
      {rules.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {rules.map((r) => (
            <span
              key={r.id}
              className="sr-num flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs"
              style={{ background: 'var(--sr-surface-2)' }}
            >
              {r.condition === 'NEW_TRADE'
                ? '새 거래'
                : `가격 변동 ±${r.threshold ?? 0}%`}
              <button
                aria-label="규칙 삭제"
                className="sr-muted hover:text-[var(--sr-up)]"
                onClick={() => removeAlert.mutate(r.id, { onError: onErr })}
              >
                ✕
              </button>
            </span>
          ))}
        </div>
      )}

      {/* add rule */}
      <div className="flex flex-wrap items-center gap-2 border-t sr-divide pt-3" style={{ borderTopWidth: '0.5px' }}>
        <button
          className="sr-input text-xs disabled:opacity-50"
          onClick={addNewTrade}
          disabled={hasNewTrade}
        >
          + 새 거래 알림
        </button>
        <div className="flex items-center gap-1">
          <span className="sr-muted text-xs">가격 변동 ±</span>
          <input
            className="sr-input sr-num w-14 text-xs"
            type="number"
            min="0.5"
            step="0.5"
            value={pct}
            onChange={(e) => setPct(e.target.value)}
          />
          <span className="sr-muted text-xs">%</span>
          <button className="sr-input text-xs" onClick={addPriceChange}>
            추가
          </button>
        </div>
      </div>
    </div>
  );
}
