import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import type { PropertyType, TradeType } from '../api/types';
import { propertyMeta } from './propertyTypes';
import { DEFAULT_LAWD_CD } from './regions';

/**
 * Selection shared across tabs (대시보드 ↔ 지도): region + 건물유형 + 거래유형.
 * Selecting a sale-only property type (토지/상업/산업/분양권) drops 전월세 back to 매매.
 */
interface FilterContextValue {
  lawdCd: string;
  propertyType: PropertyType;
  tradeType: TradeType;
  setLawdCd: (lawdCd: string) => void;
  setProperty: (pt: PropertyType) => void;
  setTradeType: (tt: TradeType) => void;
}

const FilterContext = createContext<FilterContextValue | null>(null);

export function FilterProvider({ children }: { children: ReactNode }) {
  const [lawdCd, setLawdCd] = useState(DEFAULT_LAWD_CD);
  const [propertyType, setPropertyType] = useState<PropertyType>('APT');
  const [tradeType, setTradeType] = useState<TradeType>('SALE');

  const setProperty = useCallback((pt: PropertyType) => {
    setPropertyType(pt);
    if (!propertyMeta(pt).rentAvailable) setTradeType('SALE');
  }, []);

  const value = useMemo(
    () => ({ lawdCd, propertyType, tradeType, setLawdCd, setProperty, setTradeType }),
    [lawdCd, propertyType, tradeType, setProperty],
  );

  return <FilterContext.Provider value={value}>{children}</FilterContext.Provider>;
}

export function useFilters(): FilterContextValue {
  const ctx = useContext(FilterContext);
  if (!ctx) throw new Error('useFilters must be used within FilterProvider');
  return ctx;
}
