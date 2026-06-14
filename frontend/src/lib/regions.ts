import { ALL_REGIONS } from './regionsData';

export interface Region {
  lawdCd: string;
  sido: string;
  sigungu: string;
  /** Friendly display name, e.g. "서울 강남구", "경기 성남시 분당구". */
  name: string;
}

export const REGIONS: Region[] = ALL_REGIONS;

// 성남시 분당구 — the region seeded during development; a sensible default landing.
export const DEFAULT_LAWD_CD = '41135';

const byCode = new Map(REGIONS.map((r) => [r.lawdCd, r]));

export function regionName(lawdCd: string): string {
  return byCode.get(lawdCd)?.name ?? lawdCd;
}

/** Authoritative check: is this a known South Korean 시군구 code (in the 250 master)? */
export function isKnownRegion(lawdCd: string): boolean {
  return byCode.has(lawdCd);
}

/** Substring search over name / 시군구 / 시도, capped for the autocomplete dropdown. */
export function searchRegions(query: string, limit = 30): Region[] {
  const q = query.trim();
  if (!q) return [];
  return REGIONS.filter(
    (r) => r.name.includes(q) || r.sigungu.includes(q) || r.sido.includes(q),
  ).slice(0, limit);
}
