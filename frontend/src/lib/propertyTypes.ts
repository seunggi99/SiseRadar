import type { PropertyType, TradeType } from '../api/types';

export const TRADE_TYPES: { key: TradeType; label: string }[] = [
  { key: 'SALE', label: '매매' },
  { key: 'RENT', label: '전월세' },
];

export interface PropertyMeta {
  key: PropertyType;
  label: string;
  enabled: boolean;
  /** Has building-level names → 단지 랭킹 makes sense. */
  hasRanking: boolean;
  /** 전월세(RENT) endpoint exists for this type. */
  rentAvailable: boolean;
  /** 지도에 올릴 수 있는 유형(건물명→지오코딩). 대시보드는 전유형, 지도는 이 셋만. */
  mappable: boolean;
}

export const PROPERTY_TYPES: PropertyMeta[] = [
  { key: 'APT', label: '아파트', enabled: true, hasRanking: true, rentAvailable: true, mappable: true },
  { key: 'OFFICETEL', label: '오피스텔', enabled: true, hasRanking: true, rentAvailable: true, mappable: true },
  { key: 'ROW_HOUSE', label: '연립다세대', enabled: true, hasRanking: true, rentAvailable: true, mappable: true },
  { key: 'DETACHED', label: '단독다가구', enabled: true, hasRanking: false, rentAvailable: true, mappable: false },
  { key: 'PRESALE_RIGHT', label: '분양권', enabled: true, hasRanking: true, rentAvailable: false, mappable: false },
  { key: 'LAND', label: '토지', enabled: true, hasRanking: false, rentAvailable: false, mappable: false },
  { key: 'COMMERCIAL', label: '상업업무용', enabled: true, hasRanking: false, rentAvailable: false, mappable: false },
  { key: 'INDUSTRIAL', label: '산업용', enabled: true, hasRanking: false, rentAvailable: false, mappable: false },
];

const META = new Map(PROPERTY_TYPES.map((p) => [p.key, p]));

export function propertyMeta(pt: PropertyType): PropertyMeta {
  return META.get(pt) ?? PROPERTY_TYPES[0];
}

/** 지도에서 선택 가능한 유형(주거 3종). */
export function mappableTypes(): PropertyType[] {
  return PROPERTY_TYPES.filter((p) => p.mappable).map((p) => p.key);
}

/** Label for the primary amount given a trade type. */
export function amountLabel(tradeType: TradeType): string {
  return tradeType === 'RENT' ? '평균 보증금' : '평균가';
}
