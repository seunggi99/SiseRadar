import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import {
  useAddWatchlist,
  useCollectRegion,
  useComplexRanking,
  useMonthlyStats,
  useRegionStatus,
  useSameStoreChange,
} from '../api/hooks';
import { AreaBandBreakdown } from '../components/AreaBandBreakdown';
import { ComplexChangePanel } from '../components/ComplexChangePanel';
import { ComplexDetailModal } from '../components/ComplexDetailModal';
import { ComplexRankingTable } from '../components/ComplexRankingTable';
import { Header } from '../components/Header';
import { InsightCard } from '../components/InsightCard';
import { KpiCard } from '../components/KpiCard';
import { PropertyTradeSelector } from '../components/PropertyTradeSelector';
import { RegionMapModal } from '../components/RegionMapModal';
import { RegionSearch } from '../components/RegionSearch';
import { EmptyState, ErrorState, LoadingState } from '../components/StateViews';
import { TrendChart } from '../components/TrendChart';
import { useAuth } from '../lib/auth';
import { directionColor } from '../lib/colors';
import { useFilters } from '../lib/filters';
import { amountLabel, propertyMeta } from '../lib/propertyTypes';
import { useToast } from '../lib/toast';
import {
  formatCount,
  formatEok,
  formatManwon,
  formatPercent,
  formatPerPyeong,
  formatYmLong,
  perAreaToPyeong,
} from '../lib/format';
import { regionName } from '../lib/regions';

/** 버킷 크기(개월)에 맞는 기간 명사 — 전월/전분기/전반기/전년 라벨에 사용. */
function periodNoun(bucketMonths: number): string {
  return bucketMonths === 1 ? '월' : bucketMonths === 3 ? '분기' : bucketMonths === 6 ? '반기' : '년';
}

/** 최신 버킷의 기간 표기: 1개월 "2026년 6월", 그 외 "2026.04~06" / "2025.10~2026.03". */
function bucketSpan(ym: string, bucketMonths: number): string {
  const sy = ym.slice(0, 4);
  const sm = ym.slice(4, 6);
  if (bucketMonths <= 1) return `${sy}년 ${Number(sm)}월`;
  const idx = Number(sy) * 12 + (Number(sm) - 1) + bucketMonths - 1;
  const ey = Math.floor(idx / 12);
  const em = String((idx % 12) + 1).padStart(2, '0');
  return ey === Number(sy) ? `${sy}.${sm}~${em}` : `${sy}.${sm}~${ey}.${em}`;
}

export function DashboardPage() {
  const { lawdCd, propertyType, tradeType, from, to, setLawdCd, setProperty, setTradeType } =
    useFilters();
  const [selectedComplex, setSelectedComplex] = useState<string | null>(null);
  const [mapOpen, setMapOpen] = useState(false);
  // 차트 봉 하나당 묶는 개월 수(버킷). 표시 범위는 기간 필터(from/to)가 담당.
  const [bucketMonths, setBucketMonths] = useState(1);
  const monthly = useMonthlyStats(lawdCd, propertyType, tradeType, from, to, bucketMonths);
  const ranking = useComplexRanking(lawdCd, propertyType, tradeType);
  const isRent = tradeType === 'RENT';
  const meta = propertyMeta(propertyType);

  // on-demand collection: regions with no data trigger a background 24-month backfill
  const stats = monthly.data ?? [];
  const noData = !monthly.isLoading && !monthly.isError && stats.length === 0;
  // 동일단지 변동률 — 최근 12개월 vs 직전 12개월 고정(지도·AI와 같은 단일 지표). 건물형 유형만.
  const change = useSameStoreChange(
    lawdCd,
    propertyType,
    tradeType,
    propertyMeta(propertyType).hasRanking && !noData,
  );
  const regionStatus = useRegionStatus(lawdCd, noData);
  const collectRegion = useCollectRegion();
  const triggered = useRef<Set<string>>(new Set());

  useEffect(() => {
    if (noData && regionStatus.data?.state === 'NONE' && !triggered.current.has(lawdCd)) {
      triggered.current.add(lawdCd);
      collectRegion.mutate(lawdCd);
    }
  }, [noData, regionStatus.data?.state, lawdCd, collectRegion]);

  useEffect(() => {
    if (regionStatus.data?.state === 'DONE' && regionStatus.data.months > 0) {
      monthly.refetch();
      ranking.refetch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [regionStatus.data?.state, regionStatus.data?.months]);

  const { isAuthenticated } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();
  const addWatchlist = useAddWatchlist();

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
            <RegionSearch onSelect={(r) => setLawdCd(r.lawdCd)} />
            <button className="sr-input text-sm" onClick={() => setMapOpen(true)} title="지도에서 지역 선택">
              지도
            </button>
          </div>
        </div>

        {/* property + trade type */}
        <div className="mb-6">
          <PropertyTradeSelector
            propertyType={propertyType}
            tradeType={tradeType}
            onPropertyChange={setProperty}
            onTradeChange={setTradeType}
          />
        </div>

        {monthly.isLoading ? (
          <LoadingState />
        ) : monthly.isError ? (
          <ErrorState onRetry={() => monthly.refetch()} />
        ) : noData ? (
          regionStatus.data?.state === 'DONE' && (regionStatus.data?.months ?? 0) === 0 ? (
            <EmptyState message={`${regionName(lawdCd)}는 최근 24개월 거래가 없어요.`} />
          ) : (
            <LoadingState
              message={`${regionName(lawdCd)} 데이터를 처음 불러오는 중이에요 — 수십 초 걸릴 수 있어요.`}
            />
          )
        ) : latest ? (
          <div className="flex flex-col gap-6">
            {/* KPI cards */}
            <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <KpiCard
                label={isRent ? '평균 보증금 (참고용)' : '평균 거래가 (참고용)'}
                value={formatEok(latest.avgAmount)}
                sub={formatManwon(latest.avgAmount)}
              />
              <KpiCard
                label={bucketMonths === 1 ? '거래량 (최근월)' : `거래량 (최근 ${periodNoun(bucketMonths)})`}
                value={`${formatCount(latest.count)}건`}
                sub={`${bucketSpan(latest.ym, bucketMonths)} · 전체 기간은 AI 요약 참고`}
              />
              <KpiCard
                label={`전${periodNoun(bucketMonths)} 대비`}
                value={latest.momChangePct === null ? '—' : formatPercent(latest.momChangePct)}
                valueColor={directionColor(latest.momChangePct)}
                sub="중위 ㎡당 기준"
              />
              <KpiCard
                label={isRent ? '보증금 평당 (전용)' : '평당가 (전용)'}
                value={formatPerPyeong(latest.avgPricePerArea)}
                sub={
                  isRent
                    ? `중위 ${perAreaToPyeong(latest.medianPricePerArea).toLocaleString('ko-KR')} · 월세 ${latest.avgMonthlyRent ?? 0}만`
                    : `중위 ${perAreaToPyeong(latest.medianPricePerArea).toLocaleString('ko-KR')} · ㎡당 ${Math.round(latest.avgPricePerArea).toLocaleString('ko-KR')}만`
                }
              />
            </section>

            {/* AI 시장 요약 — 계산된 통계를 자연어로 (챗봇 아님) */}
            <InsightCard
              lawdCd={lawdCd}
              propertyType={propertyType}
              tradeType={tradeType}
              enabled={!!latest}
            />

            {/* 동일단지 변동(최근 12개월 vs 직전 12개월) — 건물형 유형만, 데이터 부족도 표시 */}
            {meta.hasRanking && change.data && <ComplexChangePanel data={change.data} />}

            {/* trend chart */}
            <section className="sr-card">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-base font-medium">월별 추이</h2>
                <span className="sr-muted text-xs">
                  <span style={{ color: 'var(--sr-accent)' }}>●</span> {amountLabel(tradeType)}
                  &nbsp;&nbsp;<span className="opacity-60">▮</span> 거래량
                </span>
              </div>
              <TrendChart
                data={stats}
                amountLabel={amountLabel(tradeType)}
                bucketMonths={bucketMonths}
                onBucketChange={setBucketMonths}
              />
            </section>

            {/* area-band breakdown (latest month, 전용 기준) */}
            {latest.bands.length > 0 && (
              <section className="flex flex-col gap-3">
                <h2 className="text-base font-medium">
                  평형대별
                  <span className="sr-muted ml-2 text-sm">{formatYmLong(latest.ym)} · 전용 기준</span>
                </h2>
                <AreaBandBreakdown bands={latest.bands} />
              </section>
            )}

            {/* ranking table — only for building-level types (단독/토지/상업/산업 제외) */}
            {meta.hasRanking && (
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
                    amountLabel={amountLabel(tradeType)}
                    showMonthlyRent={isRent}
                    onAddComplex={addComplex}
                    onSelectComplex={setSelectedComplex}
                  />
                )}
              </section>
            )}
          </div>
        ) : null}

        <footer className="sr-muted mt-10 text-center text-xs">
          데이터 · 국토교통부 부동산 실거래가 (data.go.kr)
        </footer>
      </main>

      {selectedComplex && (
        <ComplexDetailModal
          lawdCd={lawdCd}
          aptName={selectedComplex}
          propertyType={propertyType}
          tradeType={tradeType}
          onClose={() => setSelectedComplex(null)}
          onAdd={addComplex}
        />
      )}

      {mapOpen && (
        <RegionMapModal
          onSelect={(cd) => setLawdCd(cd)}
          onClose={() => setMapOpen(false)}
        />
      )}
    </div>
  );
}
