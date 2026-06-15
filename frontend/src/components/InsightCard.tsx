import { useRegionInsight } from '../api/hooks';
import type { PropertyType, TradeType } from '../api/types';
import { RadarSpinner } from './RadarSpinner';

function changeText(pct: number | null): string {
  if (pct == null) return '데이터 부족';
  return `${pct > 0 ? '+' : ''}${pct}%`;
}

/**
 * 선택 지역 'AI 시장 요약' — 백엔드가 계산한 통계를 자연어로 풀어주는 그라운디드 요약.
 * 챗봇 아님(입력창 없음). 실시간·투자조언이 아님을 라벨로 드러낸다.
 */
export function InsightCard({
  lawdCd,
  propertyType,
  tradeType,
  enabled,
}: {
  lawdCd: string;
  propertyType: PropertyType;
  tradeType: TradeType;
  enabled: boolean;
}) {
  const q = useRegionInsight(lawdCd, propertyType, tradeType, enabled);
  if (!enabled) return null;
  const d = q.data;

  return (
    <section className="sr-card">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h2 className="flex items-center gap-2 text-base font-medium">
          <span
            className="rounded px-1.5 py-0.5 text-[10px] font-semibold"
            style={{ background: 'var(--sr-accent)', color: '#06241f' }}
          >
            AI
          </span>
          시장 요약
        </h2>
        {d && (
          <span className="sr-muted text-xs">
            {d.source === 'ai' ? 'AI 생성' : '템플릿 요약'}
            {d.basis.periodFrom && ` · 기준 ${d.basis.periodFrom}~${d.basis.periodTo} (${d.basis.months}개월)`}
          </span>
        )}
      </div>

      {q.isLoading ? (
        <div className="sr-muted flex items-center gap-2 text-sm">
          <RadarSpinner size={16} /> 요약 생성 중…
        </div>
      ) : q.isError ? (
        <p className="sr-muted text-sm">요약을 불러오지 못했어요.</p>
      ) : d ? (
        <>
          <p className="text-sm leading-relaxed" style={{ color: 'var(--sr-text)' }}>
            {d.summary}
          </p>
          {d.basis.hasData && (
            <p className="sr-muted mt-2 text-xs">
              기준 지표 · {d.basis.metricLabel} 평균{' '}
              <span className="sr-num">{d.basis.avgPerPyeong.toLocaleString('ko-KR')}만/평</span> · 기간 거래{' '}
              <span className="sr-num">{d.basis.totalVolume.toLocaleString('ko-KR')}건</span> · 동일단지 변동(최근 12개월 vs 직전 12개월){' '}
              <span className="sr-num">{changeText(d.basis.changeAvgPct)}</span>
              {d.basis.changeMedianPct != null && (
                <span className="sr-num"> (중위 {changeText(d.basis.changeMedianPct)})</span>
              )}
            </p>
          )}
          <p className="sr-muted mt-1 text-[11px] opacity-80">
            계산된 통계를 요약한 것으로, 실시간 시세나 투자 조언이 아닙니다.
          </p>
        </>
      ) : null}
    </section>
  );
}
