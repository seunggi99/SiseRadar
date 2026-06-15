import type { SameStoreChange } from '../api/types';
import { directionColor } from '../lib/colors';
import { formatCount, formatPercent, formatYmShort } from '../lib/format';

/**
 * 동일 단지 변동 — 같은 건물만 비교해 구성 편향을 통제한 변동률. 최근 12개월 vs 직전 12개월
 * 고정 윈도(지도·AI 요약과 같은 단일 지표). 두 윈도에 모두 거래된 단지가 없으면 데이터 부족.
 */
export function ComplexChangePanel({ data }: { data: SameStoreChange }) {
  const window =
    data.prevFrom && data.curTo
      ? `${formatYmShort(data.prevFrom)}~${formatYmShort(data.prevTo!)} → ${formatYmShort(data.curFrom!)}~${formatYmShort(data.curTo)}`
      : '';

  return (
    <section className="sr-card sr-kpi flex flex-col gap-2">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="text-base font-medium">
          동일 단지 변동
          <span className="sr-muted ml-2 text-sm">최근 12개월 대비 직전 12개월</span>
        </h2>
        <span className="sr-muted sr-num text-xs">{window}</span>
      </div>

      {!data.hasData || data.avgPct === null ? (
        <p className="sr-muted text-sm">
          두 기간(직전·최근 12개월)에 모두 거래된 동일 단지가 없어 변동률을 낼 수 없어요.
          24개월치 데이터가 쌓이면 표시됩니다. <span className="sr-num">(데이터 부족)</span>
        </p>
      ) : (
        <>
          <div className="flex flex-wrap items-baseline gap-x-6 gap-y-1">
            <span className="sr-num text-2xl font-medium" style={{ color: directionColor(data.avgPct) }}>
              {formatPercent(data.avgPct)}
            </span>
            <span className="sr-muted sr-num text-sm">
              중위 {data.medianPct === null ? '—' : formatPercent(data.medianPct)}
              {' · '}동일 {formatCount(data.matched)}단지(같은 건물)
            </span>
          </div>
          <p className="sr-muted text-xs">
            같은 건물만 비교한 단위면적가(전용) 변동 — 지도·AI 요약과 동일한 값·기간입니다.
          </p>
        </>
      )}
    </section>
  );
}
