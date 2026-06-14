import { useMemo, useState } from 'react';
import {
  Bar,
  Brush,
  CartesianGrid,
  ComposedChart,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { TooltipProps } from 'recharts';
import type { MonthlyStats } from '../api/types';
import { chartColors } from '../lib/colors';
import {
  formatCount,
  formatEok,
  formatManwon,
  formatPerPyeong,
  formatPerSqm,
  formatYmShort,
} from '../lib/format';
import { useTheme } from '../lib/theme';

interface TrendChartProps {
  data: MonthlyStats[];
  /** Label for the primary amount line (e.g. "평균가" / "평균 보증금"). */
  amountLabel?: string;
}

const RANGES = [
  { key: '3m', label: '3개월', months: 3 },
  { key: '6m', label: '6개월', months: 6 },
  { key: '12m', label: '1년', months: 12 },
  { key: 'all', label: '전체', months: 0 },
] as const;

type RangeKey = (typeof RANGES)[number]['key'];

export function TrendChart({ data, amountLabel = '평균가' }: TrendChartProps) {
  const { isDark } = useTheme();
  const c = chartColors(isDark);
  const [range, setRange] = useState<RangeKey>('12m');

  // data arrives ascending by ym; a preset trims to the last N months, the brush zooms within.
  const rows = useMemo(() => {
    const months = RANGES.find((r) => r.key === range)?.months ?? 0;
    const sliced = months > 0 ? data.slice(-months) : data;
    return sliced.map((d) => ({ ...d, label: formatYmShort(d.ym) }));
  }, [data, range]);

  const renderTooltip = ({ active, payload }: TooltipProps<number, string>) => {
    if (!active || !payload?.length) return null;
    const d = payload[0].payload as MonthlyStats;
    return (
      <div
        className="sr-num text-xs"
        style={{
          background: c.tooltipBg,
          border: `0.5px solid ${c.tooltipBorder}`,
          borderRadius: 8,
          padding: '8px 10px',
          color: 'var(--sr-text)',
        }}
      >
        <div className="mb-1 font-medium">
          {d.ym.slice(0, 4)}.{d.ym.slice(4, 6)}
        </div>
        <div>{amountLabel} {formatManwon(d.avgAmount)} <span className="sr-muted">(참고용)</span></div>
        <div>거래량 {formatCount(d.count)}건</div>
        {d.avgMonthlyRent != null && <div>평균 월세 {formatManwon(d.avgMonthlyRent)}</div>}
        <div>평당가(전용) 평균 {formatPerPyeong(d.avgPricePerArea)} · 중위 {formatPerPyeong(d.medianPricePerArea)}</div>
        <div className="sr-muted">㎡당 평균 {formatPerSqm(d.avgPricePerArea)}</div>
      </div>
    );
  };

  // show the brush only when there are enough points to make zooming useful
  const showBrush = rows.length > 8;

  return (
    <div className="flex flex-col gap-3">
      {/* range presets */}
      <div className="flex items-center gap-1 self-end">
        {RANGES.map((r) => {
          const active = r.key === range;
          const disabled = r.months > 0 && data.length <= r.months && r.key !== 'all';
          return (
            <button
              key={r.key}
              onClick={() => setRange(r.key)}
              disabled={disabled}
              className="sr-num rounded-[6px] px-2 py-1 text-xs transition-colors disabled:opacity-30"
              style={{
                background: active ? 'var(--sr-surface-2)' : 'transparent',
                color: active ? 'var(--sr-accent)' : 'var(--sr-text-muted)',
                border: `0.5px solid ${active ? 'var(--sr-accent)' : 'transparent'}`,
              }}
            >
              {r.label}
            </button>
          );
        })}
      </div>

      <ResponsiveContainer width="100%" height={280}>
        <ComposedChart data={rows} margin={{ top: 8, right: 8, bottom: 0, left: 8 }}>
          <CartesianGrid stroke={c.grid} vertical={false} />
          <XAxis
            dataKey="label"
            stroke={c.axis}
            tick={{ fontSize: 11, fill: c.axis }}
            tickLine={false}
            axisLine={{ stroke: c.grid }}
            minTickGap={16}
          />
          <YAxis
            yAxisId="price"
            stroke={c.axis}
            tick={{ fontSize: 11, fill: c.axis }}
            tickLine={false}
            axisLine={false}
            width={44}
            tickFormatter={(v: number) => formatEok(v)}
          />
          <YAxis
            yAxisId="volume"
            orientation="right"
            stroke={c.axis}
            tick={{ fontSize: 11, fill: c.axis }}
            tickLine={false}
            axisLine={false}
            width={32}
          />
          <Tooltip content={renderTooltip} cursor={{ fill: c.grid }} />
          <Bar
            yAxisId="volume"
            dataKey="count"
            fill={c.bar}
            radius={[3, 3, 0, 0]}
            barSize={rows.length > 14 ? 10 : 22}
            isAnimationActive={false}
          />
          <Line
            yAxisId="price"
            type="monotone"
            dataKey="avgAmount"
            stroke={c.line}
            strokeWidth={2}
            dot={rows.length > 14 ? false : { r: 3, fill: c.line }}
            activeDot={{ r: 5 }}
            isAnimationActive={false}
          />
          {showBrush && (
            <Brush
              dataKey="label"
              height={20}
              travellerWidth={8}
              stroke={c.axis}
              fill="transparent"
              tickFormatter={() => ''}
            />
          )}
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
