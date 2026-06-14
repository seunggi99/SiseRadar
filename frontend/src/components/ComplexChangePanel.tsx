import type { ComplexChange } from '../api/types';
import { directionColor } from '../lib/colors';
import { formatCount, formatPercent, formatYmShort } from '../lib/format';

/**
 * "진짜 추세" — 두 시점에 모두 거래된 같은 건물+평형대만 비교해 구성 편향을 통제한 변동률.
 * 단순 평균 변동(naive)과 대조해서 보여준다.
 */
export function ComplexChangePanel({ data }: { data: ComplexChange }) {
  if (data.matchedComplexes === 0 || data.sameStoreAvgChangePct === null) {
    return null;
  }
  const avg = data.sameStoreAvgChangePct;

  return (
    <section className="sr-card sr-kpi flex flex-col gap-2">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="text-base font-medium">동일 단지 추세 (구성 통제)</h2>
        <span className="sr-muted sr-num text-xs">
          {data.fromYm && data.toYm ? `${formatYmShort(data.fromYm)} → ${formatYmShort(data.toYm)}` : ''}
        </span>
      </div>

      <div className="flex flex-wrap items-baseline gap-x-6 gap-y-1">
        <span className="sr-num text-2xl font-medium" style={{ color: directionColor(avg) }}>
          {formatPercent(avg)}
        </span>
        <span className="sr-muted sr-num text-sm">
          중위 {data.sameStoreMedianChangePct === null ? '—' : formatPercent(data.sameStoreMedianChangePct)}
          {' · '}
          동일 {formatCount(data.matchedComplexes)}단지(건물+평형대)
        </span>
      </div>

      <p className="sr-muted text-xs">
        같은 건물·같은 평형대만 비교한 단위면적가 변동.
        {data.naiveChangePct !== null && (
          <>
            {' '}단순 평균 변동은{' '}
            <span className="sr-num" style={{ color: directionColor(data.naiveChangePct) }}>
              {formatPercent(data.naiveChangePct)}
            </span>{' '}
            — 차이가 거래 구성(평형대·단지 믹스) 편향입니다.
          </>
        )}
      </p>
    </section>
  );
}
