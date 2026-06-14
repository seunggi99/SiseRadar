import { useQueries } from '@tanstack/react-query';
import { useState } from 'react';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '../api/client';
import type { MonthlyStats } from '../api/types';
import { Header } from '../components/Header';
import { RegionSearch } from '../components/RegionSearch';
import { COMPARE_COLORS, chartColors } from '../lib/colors';
import { formatCount, formatEok, formatManwon, formatPercent, formatYmShort } from '../lib/format';
import { directionColor } from '../lib/colors';
import { regionName } from '../lib/regions';
import { useTheme } from '../lib/theme';

const MAX = 3;

export function ComparePage() {
  const { isDark } = useTheme();
  const c = chartColors(isDark);
  const [selected, setSelected] = useState<string[]>(['41135', '11680']);

  // Compare is on 아파트 매매 for now.
  const results = useQueries({
    queries: selected.map((lawdCd) => ({
      queryKey: ['monthly', lawdCd, 'APT', 'SALE'],
      queryFn: () => api.monthlyStats(lawdCd, 'APT', 'SALE'),
    })),
  });

  const seriesByRegion = selected.map((lawdCd, i) => ({
    lawdCd,
    name: regionName(lawdCd),
    color: COMPARE_COLORS[i % COMPARE_COLORS.length],
    stats: (results[i].data ?? []) as MonthlyStats[],
  }));

  // merge months across regions into rows: { ym, [lawdCd]: avgAmount }
  const months = Array.from(
    new Set(seriesByRegion.flatMap((s) => s.stats.map((m) => m.ym))),
  ).sort();
  // Prefix the series key — Recharts mishandles purely-numeric string dataKeys like "41135".
  const seriesKey = (lawdCd: string) => `r_${lawdCd}`;
  const chartRows = months.map((ym) => {
    const row: Record<string, number | string> = { label: formatYmShort(ym) };
    for (const s of seriesByRegion) {
      const found = s.stats.find((m) => m.ym === ym);
      if (found) row[seriesKey(s.lawdCd)] = found.avgAmount;
    }
    return row;
  });

  function add(lawdCd: string) {
    setSelected((cur) =>
      cur.includes(lawdCd) || cur.length >= MAX ? cur : [...cur, lawdCd],
    );
  }
  function remove(lawdCd: string) {
    setSelected((cur) => cur.filter((c) => c !== lawdCd));
  }

  return (
    <div className="min-h-screen">
      <Header />
      <main className="mx-auto max-w-5xl px-5 py-6">
        <h1 className="mb-1 text-xl font-medium tracking-tight">지역 비교</h1>
        <p className="sr-muted mb-5 text-sm">최대 {MAX}개 지역의 평균가 추이를 겹쳐 봅니다.</p>

        {/* selected region chips + search to add */}
        <div className="mb-6 flex flex-wrap items-center gap-2">
          {selected.map((lawdCd, idx) => (
            <span
              key={lawdCd}
              className="flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm"
              style={{
                background: 'var(--sr-surface-2)',
                border: `0.5px solid ${COMPARE_COLORS[idx % MAX]}`,
              }}
            >
              <span className="h-2 w-2 rounded-full" style={{ background: COMPARE_COLORS[idx % MAX] }} />
              {regionName(lawdCd)}
              <button
                aria-label="제거"
                className="sr-muted hover:text-[var(--sr-up)]"
                onClick={() => remove(lawdCd)}
              >
                ✕
              </button>
            </span>
          ))}
          {selected.length < MAX && (
            <RegionSearch onSelect={(r) => add(r.lawdCd)} placeholder="지역 추가" />
          )}
        </div>

        {selected.length === 0 ? (
          <div className="sr-card text-center">
            <p className="sr-muted text-sm">비교할 지역을 선택해 주세요.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-6">
            <section className="sr-card">
              <h2 className="mb-4 text-base font-medium">평균가 추이</h2>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={chartRows} margin={{ top: 8, right: 12, bottom: 0, left: 8 }}>
                  <CartesianGrid stroke={c.grid} vertical={false} />
                  <XAxis
                    dataKey="label"
                    stroke={c.axis}
                    tick={{ fontSize: 11, fill: c.axis }}
                    tickLine={false}
                    axisLine={{ stroke: c.grid }}
                  />
                  <YAxis
                    stroke={c.axis}
                    tick={{ fontSize: 11, fill: c.axis }}
                    tickLine={false}
                    axisLine={false}
                    width={48}
                    tickFormatter={(v: number) => formatEok(v)}
                  />
                  <Tooltip
                    contentStyle={{
                      background: c.tooltipBg,
                      border: `0.5px solid ${c.tooltipBorder}`,
                      borderRadius: 8,
                      fontSize: 12,
                    }}
                    formatter={(value: number, name: string) => [formatManwon(value), name]}
                  />
                  <Legend />
                  {seriesByRegion.map((s) => (
                    <Line
                      key={s.lawdCd}
                      type="monotone"
                      dataKey={seriesKey(s.lawdCd)}
                      name={s.name}
                      stroke={s.color}
                      strokeWidth={2}
                      dot={{ r: 3, fill: s.color }}
                      connectNulls
                      isAnimationActive={false}
                    />
                  ))}
                </LineChart>
              </ResponsiveContainer>
            </section>

            {/* KPI compare table */}
            <section className="sr-surface overflow-x-auto">
              <table className="w-full min-w-[520px] border-collapse text-sm">
                <thead style={{ background: 'var(--sr-surface-2)' }}>
                  <tr className="sr-muted">
                    <th className="px-3 py-2.5 text-left font-medium">지역</th>
                    <th className="px-3 py-2.5 text-right font-medium">평균가</th>
                    <th className="px-3 py-2.5 text-right font-medium">중위가</th>
                    <th className="px-3 py-2.5 text-right font-medium">평당가</th>
                    <th className="px-3 py-2.5 text-right font-medium">거래량</th>
                    <th className="px-3 py-2.5 text-right font-medium">전월비</th>
                  </tr>
                </thead>
                <tbody>
                  {seriesByRegion.map((s) => {
                    const latest = s.stats.length ? s.stats[s.stats.length - 1] : null;
                    return (
                      <tr key={s.lawdCd} className="border-t sr-divide" style={{ borderTopWidth: '0.5px' }}>
                        <td className="px-3 py-2.5">
                          <span className="flex items-center gap-2">
                            <span className="h-2 w-2 rounded-full" style={{ background: s.color }} />
                            {s.name}
                          </span>
                        </td>
                        {latest ? (
                          <>
                            <td className="sr-num px-3 py-2.5 text-right">{formatEok(latest.avgAmount)}</td>
                            <td className="sr-num px-3 py-2.5 text-right">{formatEok(latest.medianAmount)}</td>
                            <td className="sr-num px-3 py-2.5 text-right">{formatCount(latest.avgPricePerPyeong)}만/평</td>
                            <td className="sr-num px-3 py-2.5 text-right">{formatCount(latest.count)}</td>
                            <td
                              className="sr-num px-3 py-2.5 text-right"
                              style={{ color: directionColor(latest.momChangePct) }}
                            >
                              {latest.momChangePct === null ? '—' : formatPercent(latest.momChangePct)}
                            </td>
                          </>
                        ) : (
                          <td colSpan={5} className="sr-muted px-3 py-2.5 text-right text-xs">
                            수집된 데이터 없음
                          </td>
                        )}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
