// All money values arrive in 만원.

/** Compact price, e.g. 152138 → "15.2억", 9580 → "9,580만". */
export function formatEok(manwon: number): string {
  if (manwon >= 10000) {
    const eok = manwon / 10000;
    return `${eok.toFixed(eok >= 10 ? 1 : 2)}억`;
  }
  return `${Math.round(manwon).toLocaleString('ko-KR')}만`;
}

/** Full 만원 amount, e.g. 152138 → "152,138만원". */
export function formatManwon(manwon: number): string {
  return `${Math.round(manwon).toLocaleString('ko-KR')}만원`;
}

/** Price per pyeong, e.g. 7435 → "7,435만/평". */
export function formatPyeongPrice(manwon: number): string {
  return `${Math.round(manwon).toLocaleString('ko-KR')}만/평`;
}

/** Signed percent, e.g. 4.7 → "+4.7%", -4.5 → "-4.5%". */
export function formatPercent(pct: number): string {
  return `${pct > 0 ? '+' : ''}${pct.toFixed(1)}%`;
}

/** "202603" → "2026년 3월". */
export function formatYmLong(ym: string): string {
  return `${ym.slice(0, 4)}년 ${Number(ym.slice(4, 6))}월`;
}

/** "202603" → "26.03" (compact, for chart axes). */
export function formatYmShort(ym: string): string {
  return `${ym.slice(2, 4)}.${ym.slice(4, 6)}`;
}

export function formatCount(n: number): string {
  return n.toLocaleString('ko-KR');
}
