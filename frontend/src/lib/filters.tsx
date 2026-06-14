import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import type { AreaBandKey, PropertyType, TradeType } from '../api/types';
import { propertyMeta } from './propertyTypes';
import { DEFAULT_LAWD_CD } from './regions';

/**
 * Selection shared across tabs (대시보드 ↔ 지도): region + 건물유형 + 거래유형 + 기간 + 평형대.
 * - sale-only 유형(토지/상업/산업/분양권) 선택 시 전월세→매매 자동.
 * - from/to 미지정(undefined)이면 서버가 sensible default(최근 N개월)로 채움 → 지도가 비지 않게.
 * - band null = 전체 평형대.
 */
interface FilterContextValue {
  lawdCd: string;
  propertyType: PropertyType;
  tradeType: TradeType;
  from?: string;
  to?: string;
  band: AreaBandKey | null;
  setLawdCd: (lawdCd: string) => void;
  setProperty: (pt: PropertyType) => void;
  setTradeType: (tt: TradeType) => void;
  setPeriod: (from?: string, to?: string) => void;
  setBand: (band: AreaBandKey | null) => void;
}

const FilterContext = createContext<FilterContextValue | null>(null);

export function FilterProvider({ children }: { children: ReactNode }) {
  const [lawdCd, setLawdCd] = useState(DEFAULT_LAWD_CD);
  const [propertyType, setPropertyType] = useState<PropertyType>('APT');
  const [tradeType, setTradeType] = useState<TradeType>('SALE');
  const [from, setFrom] = useState<string | undefined>(undefined);
  const [to, setTo] = useState<string | undefined>(undefined);
  const [band, setBand] = useState<AreaBandKey | null>(null);

  const setProperty = useCallback((pt: PropertyType) => {
    setPropertyType(pt);
    if (!propertyMeta(pt).rentAvailable) setTradeType('SALE');
  }, []);

  const setPeriod = useCallback((f?: string, t?: string) => {
    setFrom(f);
    setTo(t);
  }, []);

  const value = useMemo(
    () => ({
      lawdCd,
      propertyType,
      tradeType,
      from,
      to,
      band,
      setLawdCd,
      setProperty,
      setTradeType,
      setPeriod,
      setBand,
    }),
    [lawdCd, propertyType, tradeType, from, to, band, setProperty, setPeriod],
  );

  return <FilterContext.Provider value={value}>{children}</FilterContext.Provider>;
}

export function useFilters(): FilterContextValue {
  const ctx = useContext(FilterContext);
  if (!ctx) throw new Error('useFilters must be used within FilterProvider');
  return ctx;
}
