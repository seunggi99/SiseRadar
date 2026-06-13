import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAddWatchlist, useComplexRanking, useMonthlyStats } from '../api/hooks';
import { ComplexDetailModal } from '../components/ComplexDetailModal';
import { ComplexRankingTable } from '../components/ComplexRankingTable';
import { Header } from '../components/Header';
import { KpiCard } from '../components/KpiCard';
import { RegionSelector } from '../components/RegionSelector';
import { EmptyState, ErrorState, LoadingState } from '../components/StateViews';
import { TrendChart } from '../components/TrendChart';
import { useAuth } from '../lib/auth';
import { directionColor } from '../lib/colors';
import { useToast } from '../lib/toast';
import {
  formatCount,
  formatEok,
  formatManwon,
  formatPercent,
  formatPyeongPrice,
  formatYmLong,
} from '../lib/format';
import { DEFAULT_LAWD_CD, regionName } from '../lib/regions';

export function DashboardPage() {
  const [lawdCd, setLawdCd] = useState(DEFAULT_LAWD_CD);
  const [selectedComplex, setSelectedComplex] = useState<string | null>(null);
  const monthly = useMonthlyStats(lawdCd);
  const ranking = useComplexRanking(lawdCd);

  const { isAuthenticated } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();
  const addWatchlist = useAddWatchlist();

  const stats = monthly.data ?? [];
  const latest = stats.length ? stats[stats.length - 1] : null;

  function requireLogin(): boolean {
    if (isAuthenticated) return true;
    toast('관심 등록은 로그인이 필요해요');
    navigate('/login');
    return false;
  }

  function addRegion() {
    if (!requireLogin()) return;
    addWatchlist.mutate(
      { type: 'REGION', lawdCd },
      {
        onSuccess: () => toast(`${regionName(lawdCd)} 관심 지역 추가됨`),
        onError: (e) =>
          toast(e instanceof ApiError && e.status === 409 ? '이미 추가된 지역이에요' : '추가 실패', 'error'),
      },
    );
  }

  function addComplex(aptName: string) {
    if (!requireLogin()) return;
    addWatchlist.mutate(
      { type: 'COMPLEX', lawdCd, aptName },
      {
        onSuccess: () => toast(`${aptName} 관심 단지 추가됨`),
        onError: (e) =>
          toast(e instanceof ApiError && e.status === 409 ? '이미 추가된 단지예요' : '추가 실패', 'error'),
      },
    );
  }

  return (
    <div className="min-h-screen">
      <Header />
      <main className="mx-auto max-w-6xl px-5 py-6">
        {/* region bar */}
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div className="flex flex-col gap-1">
            <span className="sr-label">지역</span>
            <h1 className="text-xl font-medium tracking-tight">{regionName(lawdCd)}</h1>
          </div>
          <div className="flex items-center gap-3">
            {latest && (
              <span className="sr-muted text-sm">기준 {formatYmLong(latest.ym)}</span>
            )}
            <button className="sr-input text-sm" onClick={addRegion} title="이 지역을 관심목록에 추가">
              + 관심 지역
            </button>
            <RegionSelector value={lawdCd} onChange={setLawdCd} />
          </div>
        </div>

        {monthly.isLoading ? (
          <LoadingState />
        ) : monthly.isError ? (
          <ErrorState onRetry={() => monthly.refetch()} />
        ) : !latest ? (
          <EmptyState
            message={`${regionName(lawdCd)}는 아직 수집된 거래가 없어요. 다른 지역을 선택하거나 수집을 실행해 주세요.`}
          />
        ) : (
          <div className="flex flex-col gap-6">
            {/* KPI cards */}
            <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <KpiCard
                label="이번 달 평균가"
                value={formatEok(latest.avgAmount)}
                sub={formatManwon(latest.avgAmount)}
              />
              <KpiCard label="거래량" value={`${formatCount(latest.count)}건`} />
              <KpiCard
                label="전월 대비"
                value={latest.momChangePct === null ? '—' : formatPercent(latest.momChangePct)}
                valueColor={directionColor(latest.momChangePct)}
                sub="평균가 기준"
              />
              <KpiCard
                label="평당가"
                value={formatPyeongPrice(latest.avgPricePerPyeong)}
                sub="전용면적 기준"
              />
            </section>

            {/* trend chart */}
            <section className="sr-card">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-base font-medium">월별 추이</h2>
                <span className="sr-muted text-xs">
                  <span style={{ color: 'var(--sr-accent)' }}>●</span> 평균가&nbsp;&nbsp;
                  <span className="opacity-60">▮</span> 거래량
                </span>
              </div>
              <TrendChart data={stats} />
            </section>

            {/* ranking table */}
            <section className="flex flex-col gap-3">
              <h2 className="text-base font-medium">
                단지 랭킹
                {latest && <span className="sr-muted ml-2 text-sm">{formatYmLong(latest.ym)}</span>}
              </h2>
              {ranking.isLoading ? (
                <LoadingState message="단지 랭킹을 집계하는 중…" />
              ) : ranking.isError ? (
                <ErrorState onRetry={() => ranking.refetch()} />
              ) : (
                <ComplexRankingTable
                  rows={ranking.data ?? []}
                  onAddComplex={addComplex}
                  onSelectComplex={setSelectedComplex}
                />
              )}
            </section>
          </div>
        )}

        <footer className="sr-muted mt-10 text-center text-xs">
          데이터 · 국토교통부 아파트 매매 실거래가 (data.go.kr)
        </footer>
      </main>

      {selectedComplex && (
        <ComplexDetailModal
          lawdCd={lawdCd}
          aptName={selectedComplex}
          onClose={() => setSelectedComplex(null)}
          onAdd={addComplex}
        />
      )}
    </div>
  );
}
