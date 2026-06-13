import { useEffect } from 'react';
import {
  CartesianGrid,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { TooltipProps } from 'recharts';
import { useComplexTrades } from '../api/hooks';
import type { Trade } from '../api/types';
import { chartColors } from '../lib/colors';
import { formatCount, formatEok, formatManwon, formatPyeongPrice } from '../lib/format';
import { useTheme } from '../lib/theme';
import { LoadingState } from './StateViews';

interface Props {
  lawdCd: string;
  aptName: string;
  onClose: () => void;
  onAdd?: (aptName: string) => void;
}

export function ComplexDetailModal({ lawdCd, aptName, onClose, onAdd }: Props) {
  const { isDark } = useTheme();
  const c = chartColors(isDark);
  const query = useComplexTrades(lawdCd, aptName);
  const trades = query.data?.content ?? [];

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  const summary = computeSummary(trades);

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto p-4 sm:p-8"
      style={{ background: 'rgba(0,0,0,0.45)' }}
      onClick={onClose}
    >
      <div
        className="sr-surface my-auto w-full max-w-2xl"
        style={{ boxShadow: '0 12px 40px rgba(0,0,0,0.3)' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* header */}
        <div
          className="flex items-start justify-between gap-3 border-b sr-divide px-5 py-4"
          style={{ borderBottomWidth: '0.5px' }}
        >
          <div className="flex flex-col gap-0.5">
            <h2 className="text-lg font-medium">{aptName}</h2>
            <span className="sr-muted text-xs">
              {summary ? `${summary.umdNm ?? lawdCd} · ${summary.buildYear ?? '-'}년 준공` : lawdCd}
            </span>
          </div>
          <div className="flex items-center gap-2">
            {onAdd && (
              <button
                className="sr-input text-xs hover:text-[var(--sr-accent)]"
                onClick={() => onAdd(aptName)}
              >
                + 관심
              </button>
            )}
            <button className="sr-muted text-lg leading-none hover:text-[var(--sr-text)]" onClick={onClose} aria-label="닫기">
              ✕
            </button>
          </div>
        </div>

        {query.isLoading ? (
          <LoadingState />
        ) : !summary ? (
          <p className="sr-muted px-5 py-12 text-center text-sm">거래 내역이 없어요.</p>
        ) : (
          <div className="flex flex-col gap-5 px-5 py-5">
            {/* KPI row */}
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <Kpi label="거래" value={`${formatCount(summary.count)}건`} />
              <Kpi label="평균가" value={formatEok(summary.avg)} />
              <Kpi label="최고가" value={formatEok(summary.max)} />
              <Kpi label="평당가" value={formatPyeongPrice(summary.avgPyeong)} />
            </div>

            {/* area vs price scatter */}
            <div>
              <h3 className="mb-2 text-sm font-medium">면적 · 가격 분포</h3>
              <ResponsiveContainer width="100%" height={220}>
                <ScatterChart margin={{ top: 8, right: 12, bottom: 4, left: 8 }}>
                  <CartesianGrid stroke={c.grid} />
                  <XAxis
                    type="number"
                    dataKey="area"
                    name="면적"
                    unit="㎡"
                    stroke={c.axis}
                    tick={{ fontSize: 11, fill: c.axis }}
                    tickLine={false}
                    axisLine={{ stroke: c.grid }}
                  />
                  <YAxis
                    type="number"
                    dataKey="dealAmount"
                    name="금액"
                    stroke={c.axis}
                    tick={{ fontSize: 11, fill: c.axis }}
                    tickLine={false}
                    axisLine={false}
                    width={44}
                    tickFormatter={(v: number) => formatEok(v)}
                  />
                  <Tooltip content={<ScatterTooltip border={c.tooltipBorder} bg={c.tooltipBg} />} />
                  <Scatter data={trades} fill={c.line} fillOpacity={0.7} isAnimationActive={false} />
                </ScatterChart>
              </ResponsiveContainer>
            </div>

            {/* transactions */}
            <div>
              <h3 className="mb-2 text-sm font-medium">최근 거래</h3>
              <div className="max-h-64 overflow-y-auto">
                <table className="w-full border-collapse text-sm">
                  <thead className="sticky top-0" style={{ background: 'var(--sr-surface-2)' }}>
                    <tr className="sr-muted">
                      <th className="px-2 py-2 text-left font-medium">거래일</th>
                      <th className="px-2 py-2 text-right font-medium">전용</th>
                      <th className="px-2 py-2 text-right font-medium">층</th>
                      <th className="px-2 py-2 text-right font-medium">거래가</th>
                    </tr>
                  </thead>
                  <tbody>
                    {trades.map((t) => (
                      <tr key={t.id} className="border-t sr-divide" style={{ borderTopWidth: '0.5px' }}>
                        <td className="sr-num px-2 py-2">{t.dealDate}</td>
                        <td className="sr-num px-2 py-2 text-right">
                          {t.area}㎡<span className="sr-muted"> ({t.areaPyeong}평)</span>
                        </td>
                        <td className="sr-num px-2 py-2 text-right sr-muted">{t.floor}</td>
                        <td className="sr-num px-2 py-2 text-right">{formatManwon(t.dealAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: string }) {
  return (
    <div className="sr-card sr-kpi flex flex-col gap-1 !p-3">
      <span className="sr-label !text-xs">{label}</span>
      <span className="sr-num text-lg font-medium">{value}</span>
    </div>
  );
}

interface Summary {
  count: number;
  avg: number;
  max: number;
  avgPyeong: number;
  buildYear: number | null;
  umdNm: string | null;
}

function computeSummary(trades: Trade[]): Summary | null {
  if (!trades.length) return null;
  const count = trades.length;
  const avg = trades.reduce((s, t) => s + t.dealAmount, 0) / count;
  const max = Math.max(...trades.map((t) => t.dealAmount));
  const avgPyeong =
    trades.reduce((s, t) => s + t.dealAmount / t.areaPyeong, 0) / count;
  return {
    count,
    avg,
    max,
    avgPyeong,
    buildYear: trades[0].buildYear,
    umdNm: trades[0].umdNm,
  };
}

function ScatterTooltip({
  active,
  payload,
  bg,
  border,
}: TooltipProps<number, string> & { bg: string; border: string }) {
  if (!active || !payload?.length) return null;
  const t = payload[0].payload as Trade;
  return (
    <div
      className="sr-num text-xs"
      style={{ background: bg, border: `0.5px solid ${border}`, borderRadius: 8, padding: '8px 10px', color: 'var(--sr-text)' }}
    >
      <div>{t.area}㎡ · {t.floor}층</div>
      <div>{formatManwon(t.dealAmount)}</div>
      <div className="sr-muted">{t.dealDate}</div>
    </div>
  );
}
