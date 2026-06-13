export interface Region {
  lawdCd: string;
  name: string;
}

// Phase 1 collects 41135 (성남시 분당구). The others are wired so the selector is real;
// regions without collected data render the empty state. A 법정동 master table arrives in Phase 4.
export const REGIONS: Region[] = [
  { lawdCd: '41135', name: '성남시 분당구' },
  { lawdCd: '11680', name: '서울 강남구' },
  { lawdCd: '11650', name: '서울 서초구' },
  { lawdCd: '41131', name: '성남시 수정구' },
];

export const DEFAULT_LAWD_CD = '41135';

export function regionName(lawdCd: string): string {
  return REGIONS.find((r) => r.lawdCd === lawdCd)?.name ?? lawdCd;
}
