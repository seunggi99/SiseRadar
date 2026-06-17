import { useMemo } from 'react';
import {
  Bar,
  Brush,
  CartesianGrid,
  Cell,
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
  /** 봉 하나당 묶는 개월 수(1/3/6/12). */
  bucketMonths: number;
  onBucketChange: (m: number) => void;
}

const BUCKETS = [
  { m: 1, label: '1개월' },
  { m: 3, label: '3개월' },
  { m: 6, label: '6개월' },
  { m: 12, label: '1년' },
] as const;

/** YYYYMM + n개월 → YYYYMM. */
function addMonths(ym: string, n: number): string {
  const idx = +ym.slice(0, 4) * 12 + (+ym.slice(4, 6) - 1) + n;
  return `${Math.floor(idx / 12)}${String((idx % 12) + 1).padStart(2, '0')}`;
}

/** 버킷 기간 라벨: 1개월 "2025.01", 그 외 "2025.01~03"(연 같으면) / "2025.10~2026.03". */
function periodLabel(ym: string, bucket: number): string {
  const sy = ym.slice(0, 4);
  const sm = ym.slice(4, 6);
  if (bucket <= 1) return `${sy}.${sm}`;
  const end = addMonths(ym, bucket - 1);
  const ey = end.slice(0, 4);
  const em = end.slice(4, 6);
  return sy === ey ? `${sy}.${sm}~${em}` : `${sy}.${sm}~${ey}.${em}`;
}

export function TrendChart({ data, amountLabel = '평균가', bucketMonths, onBucketChange }: TrendChartProps) {
  const { isDark } = useTheme();
  const c = chartColors(isDark);

  // data는 서버가 이미 버킷팅·범위 적용해 ascending(ym)으로 내려줌. 여기선 라벨/부분여부만 부여.
  const rows = useMemo(() => {
    const now = new Date();
    const nowYm = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`;
    return data.map((d) => {
      const end = addMonths(d.ym, bucketMonths - 1);
      // 부분 버킷: 데이터 개월수가 모자라거나, 진행 중인 현재월을 포함.
      const partial = d.monthsInBucket < bucketMonths || (d.ym <= nowYm && end >= nowYm);
      return { ...d, label: formatYmShort(d.ym), period: periodLabel(d.ym, bucketMonths), partial };
    });
  }, [data, bucketMonths]);

  const renderTooltip = ({ active, payload }: TooltipProps<number, string>) => {
    if (!active || !payload?.length) return null;
    const d = payload[0].payload as (typeof rows)[number];
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
          {d.period}
          {d.partial && <span className="sr-muted"> · 부분({d.monthsInBucket}개월)</span>}
        </div>
        <div>거래량(표본) {formatCount(d.count)}건</div>
        <div>
          평당가(전용) 중위 {formatPerPyeong(d.medianPricePerArea)} · 가중평균{' '}
          {formatPerPyeong(d.avgPricePerArea)}
        </div>
        <div className="sr-muted">㎡당 중위 {formatPerSqm(d.medianPricePerArea)}</div>
        {d.avgMonthlyRent != null && <div>평균 월세 {formatManwon(d.avgMonthlyRent)}</div>}
        <div className="sr-muted">
          {amountLabel} {formatManwon(d.avgAmount)} (참고용)
        </div>
      </div>
    );
  };

  const showBrush = rows.length > 8;

  return (
    <div className="flex flex-col gap-3">
      {/* 버킷 크기 토글 (봉 하나당 묶는 개월 수) */}
      <div className="flex items-center gap-1 self-end">
        <span className="sr-muted mr-1 text-xs">묶기</span>
        {BUCKETS.map((b) => {
          const active = b.m === bucketMonths;
          return (
            <button
              key={b.m}
              onClick={() => onBucketChange(b.m)}
              className="sr-num rounded-[6px] px-2 py-1 text-xs transition-colors"
              style={{
                background: active ? 'var(--sr-surface-2)' : 'transparent',
                color: active ? 'var(--sr-accent)' : 'var(--sr-text-muted)',
                border: `0.5px solid ${active ? 'var(--sr-accent)' : 'transparent'}`,
              }}
            >
              {b.label}
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
            radius={[3, 3, 0, 0]}
            barSize={rows.length > 14 ? 10 : 22}
            isAnimationActive={false}
          >
            {rows.map((r, i) => (
              // 부분 버킷은 옅게 — 0으로 위조하지 않고 '불완전'임을 시각적으로 표시.
              <Cell key={i} fill={c.bar} fillOpacity={r.partial ? 0.35 : 1} />
            ))}
          </Bar>
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
