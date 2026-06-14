import type { AreaBand, AreaBandKey } from '../api/types';
import { formatCount, formatPerPyeong, formatPerSqm } from '../lib/format';

const BAND_LABELS: Record<AreaBandKey, string> = {
  SMALL: '소형 (≤60㎡)',
  MID_SMALL: '중소형 (60–85㎡)',
  MID_LARGE: '중대형 (85–135㎡)',
  LARGE: '대형 (>135㎡)',
};
const ORDER: AreaBandKey[] = ['SMALL', 'MID_SMALL', 'MID_LARGE', 'LARGE'];

export function AreaBandBreakdown({ bands }: { bands: AreaBand[] }) {
  const byKey = new Map(bands.map((b) => [b.band, b]));
  const totalCount = bands.reduce((s, b) => s + b.count, 0) || 1;

  return (
    <div className="sr-surface overflow-hidden">
      <table className="w-full min-w-[460px] border-collapse text-sm">
        <thead style={{ background: 'var(--sr-surface-2)' }}>
          <tr className="sr-muted">
            <th className="px-3 py-2.5 text-left font-medium">평형대 (전용)</th>
            <th className="px-3 py-2.5 text-right font-medium">거래량</th>
            <th className="px-3 py-2.5 text-right font-medium">평당 평균</th>
            <th className="px-3 py-2.5 text-right font-medium">평당 중위</th>
            <th className="px-3 py-2.5 text-right font-medium">㎡당 평균</th>
          </tr>
        </thead>
        <tbody>
          {ORDER.map((key) => {
            const b = byKey.get(key);
            const share = b ? Math.round((b.count / totalCount) * 100) : 0;
            return (
              <tr key={key} className="border-t sr-divide" style={{ borderTopWidth: '0.5px' }}>
                <td className="px-3 py-2.5">{BAND_LABELS[key]}</td>
                <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right">
                  {b ? `${formatCount(b.count)}건` : '—'}
                  {b && <span className="sr-muted"> ({share}%)</span>}
                </td>
                <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right">
                  {b ? formatPerPyeong(b.avgPricePerArea) : '—'}
                </td>
                <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right sr-muted">
                  {b ? formatPerPyeong(b.medianPricePerArea) : '—'}
                </td>
                <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right sr-muted">
                  {b ? formatPerSqm(b.avgPricePerArea) : '—'}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
