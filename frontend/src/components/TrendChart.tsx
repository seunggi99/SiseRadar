import {
  Bar,
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
import { formatCount, formatEok, formatManwon, formatYmShort } from '../lib/format';
import { useTheme } from '../lib/theme';

interface TrendChartProps {
  data: MonthlyStats[];
}

export function TrendChart({ data }: TrendChartProps) {
  const { isDark } = useTheme();
  const c = chartColors(isDark);

  const rows = data.map((d) => ({ ...d, label: formatYmShort(d.ym) }));

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
        <div className="mb-1 font-medium">{d.ym.slice(0, 4)}.{d.ym.slice(4, 6)}</div>
        <div>평균가 {formatManwon(d.avgAmount)}</div>
        <div>거래량 {formatCount(d.count)}건</div>
        <div>평당가 {formatManwon(d.avgPricePerPyeong)}/평</div>
      </div>
    );
  };

  return (
    <ResponsiveContainer width="100%" height={280}>
      <ComposedChart data={rows} margin={{ top: 8, right: 8, bottom: 0, left: 8 }}>
        <CartesianGrid stroke={c.grid} vertical={false} />
        <XAxis
          dataKey="label"
          stroke={c.axis}
          tick={{ fontSize: 11, fill: c.axis }}
          tickLine={false}
          axisLine={{ stroke: c.grid }}
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
          barSize={22}
          isAnimationActive={false}
        />
        <Line
          yAxisId="price"
          type="monotone"
          dataKey="avgAmount"
          stroke={c.line}
          strokeWidth={2}
          dot={{ r: 3, fill: c.line }}
          activeDot={{ r: 5 }}
          isAnimationActive={false}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
