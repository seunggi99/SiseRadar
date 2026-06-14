import type { PropertyType, TradeType } from '../api/types';
import { PROPERTY_TYPES, TRADE_TYPES, propertyMeta } from '../lib/propertyTypes';

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
  const rentAvailable = propertyMeta(propertyType).rentAvailable;

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

      {/* 매매 / 전월세 segmented toggle (전월세 disabled for sale-only types) */}
      <div className="sr-surface flex overflow-hidden p-0.5">
        {TRADE_TYPES.map((t) => {
          const active = t.key === tradeType;
          const disabled = t.key === 'RENT' && !rentAvailable;
          return (
            <button
              key={t.key}
              onClick={() => !disabled && onTradeChange(t.key)}
              disabled={disabled}
              className="rounded-[7px] px-3 py-1 text-sm transition-colors disabled:opacity-30"
              style={{
                background: active ? 'var(--sr-accent)' : 'transparent',
                color: active ? 'var(--sr-navy)' : 'var(--sr-text-muted)',
              }}
              title={disabled ? '이 유형은 전월세 데이터가 없어요' : undefined}
            >
              {t.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
