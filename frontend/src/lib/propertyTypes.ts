import type { PropertyType, TradeType } from '../api/types';

export const TRADE_TYPES: { key: TradeType; label: string }[] = [
  { key: 'SALE', label: '매매' },
  { key: 'RENT', label: '전월세' },
];

// Only APT is collected today; others are shown disabled ("준비중") until enabled in the backend.
export const PROPERTY_TYPES: { key: PropertyType; label: string; enabled: boolean }[] = [
  { key: 'APT', label: '아파트', enabled: true },
  { key: 'OFFICETEL', label: '오피스텔', enabled: false },
  { key: 'ROW_HOUSE', label: '연립다세대', enabled: false },
  { key: 'DETACHED', label: '단독다가구', enabled: false },
];

/** Label for the primary amount given a trade type. */
export function amountLabel(tradeType: TradeType): string {
  return tradeType === 'RENT' ? '평균 보증금' : '평균가';
}
