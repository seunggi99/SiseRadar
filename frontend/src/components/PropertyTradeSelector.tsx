import type { PropertyType, TradeType } from '../api/types';
import { PROPERTY_TYPES, TRADE_TYPES } from '../lib/propertyTypes';

interface Props {
  propertyType: PropertyType;
  tradeType: TradeType;
  onPropertyChange: (pt: PropertyType) => void;
  onTradeChange: (tt: TradeType) => void;
}

export function PropertyTradeSelector({
  propertyType,
  tradeType,
  onPropertyChange,
  onTradeChange,
}: Props) {
  return (
    <div className="flex items-center gap-2">
      <select
        className="sr-input text-sm"
        value={propertyType}
        onChange={(e) => onPropertyChange(e.target.value as PropertyType)}
        aria-label="부동산 유형"
      >
        {PROPERTY_TYPES.map((p) => (
          <option key={p.key} value={p.key} disabled={!p.enabled}>
            {p.label}
            {p.enabled ? '' : ' (준비중)'}
          </option>
        ))}
      </select>

      {/* 매매 / 전월세 segmented toggle */}
      <div className="sr-surface flex overflow-hidden p-0.5">
        {TRADE_TYPES.map((t) => {
          const active = t.key === tradeType;
          return (
            <button
              key={t.key}
              onClick={() => onTradeChange(t.key)}
              className="rounded-[7px] px-3 py-1 text-sm transition-colors"
              style={{
                background: active ? 'var(--sr-accent)' : 'transparent',
                color: active ? 'var(--sr-navy)' : 'var(--sr-text-muted)',
              }}
            >
              {t.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
