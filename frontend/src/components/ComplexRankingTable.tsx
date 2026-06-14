import { useMemo, useState } from 'react';
import type { ComplexRank } from '../api/types';
import { formatCount, formatEok, formatPyeongPrice } from '../lib/format';

type SortKey = 'rank' | 'avgAmount' | 'count' | 'avgPricePerPyeong' | 'maxAmount' | 'avgMonthlyRent';

interface ComplexRankingTableProps {
  rows: ComplexRank[];
  /** Label for the primary amount column (e.g. "평균가" / "평균 보증금"). */
  amountLabel?: string;
  /** Show the 월세 column (rent). */
  showMonthlyRent?: boolean;
  /** When provided, each row shows a "관심 추가" button calling this with the complex name. */
  onAddComplex?: (aptName: string) => void;
  /** When provided, the complex name becomes a button opening the detail view. */
  onSelectComplex?: (aptName: string) => void;
}

export function ComplexRankingTable({
  rows,
  amountLabel = '평균가',
  showMonthlyRent = false,
  onAddComplex,
  onSelectComplex,
}: ComplexRankingTableProps) {
  const [query, setQuery] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('rank');
  const [asc, setAsc] = useState(true);

  const columns: { key: SortKey; label: string }[] = [
    { key: 'avgAmount', label: amountLabel },
    { key: 'maxAmount', label: '최고가' },
    { key: 'avgPricePerPyeong', label: '평당가' },
    ...(showMonthlyRent ? [{ key: 'avgMonthlyRent' as SortKey, label: '월세' }] : []),
    { key: 'count', label: '거래량' },
  ];

  const filtered = useMemo(() => {
    const q = query.trim();
    const base = q ? rows.filter((r) => r.aptName.includes(q)) : rows;
    return [...base].sort((a, b) => {
      const av = a[sortKey] ?? 0;
      const bv = b[sortKey] ?? 0;
      const diff = av - bv;
      return asc ? diff : -diff;
    });
  }, [rows, query, sortKey, asc]);

  function toggleSort(key: SortKey) {
    if (key === sortKey) {
      setAsc((v) => !v);
    } else {
      setSortKey(key);
      setAsc(key === 'rank');
    }
  }

  const colSpan = 1 + columns.length + (onAddComplex ? 1 : 0);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-3">
        <input
          className="sr-input w-full max-w-xs text-sm"
          placeholder="단지명 검색"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <span className="sr-muted shrink-0 text-xs">{formatCount(filtered.length)}개 단지</span>
      </div>

      <div className="sr-surface overflow-hidden">
        <div className="max-h-[480px] overflow-auto">
          <table className="w-full min-w-[480px] border-collapse text-sm">
            <thead className="sticky top-0" style={{ background: 'var(--sr-surface-2)' }}>
              <tr>
                <th className="px-3 py-2.5 text-left font-medium sr-muted">단지</th>
                {columns.map((col) => (
                  <th
                    key={col.key}
                    className="cursor-pointer select-none whitespace-nowrap px-3 py-2.5 text-right font-medium sr-muted hover:text-[var(--sr-text)]"
                    onClick={() => toggleSort(col.key)}
                  >
                    {col.label}
                    {sortKey === col.key && <span className="ml-1">{asc ? '↑' : '↓'}</span>}
                  </th>
                ))}
                {onAddComplex && <th className="px-3 py-2.5" aria-label="관심 추가" />}
              </tr>
            </thead>
            <tbody>
              {filtered.map((r) => (
                <tr key={r.aptName} className="border-t sr-divide" style={{ borderTopWidth: '0.5px' }}>
                  <td className="px-3 py-2.5">
                    <div className="flex items-center gap-2.5">
                      <span
                        className="sr-num w-6 shrink-0 text-right text-xs"
                        style={{ color: 'var(--sr-accent)' }}
                      >
                        {r.rank}
                      </span>
                      {onSelectComplex ? (
                        <button
                          className="truncate text-left hover:text-[var(--sr-accent)] hover:underline"
                          title={r.aptName}
                          onClick={() => onSelectComplex(r.aptName)}
                        >
                          {r.aptName}
                        </button>
                      ) : (
                        <span className="truncate" title={r.aptName}>
                          {r.aptName}
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right">{formatEok(r.avgAmount)}</td>
                  <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right sr-muted">{formatEok(r.maxAmount)}</td>
                  <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right">{formatPyeongPrice(r.avgPricePerPyeong)}</td>
                  {showMonthlyRent && (
                    <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right sr-muted">
                      {formatCount(r.avgMonthlyRent ?? 0)}만
                    </td>
                  )}
                  <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right sr-muted">{formatCount(r.count)}</td>
                  {onAddComplex && (
                    <td className="px-2 py-2.5 text-right">
                      <button
                        className="sr-muted text-xs hover:text-[var(--sr-accent)]"
                        title="관심 단지 추가"
                        onClick={() => onAddComplex(r.aptName)}
                      >
                        + 관심
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={colSpan} className="sr-muted px-3 py-8 text-center text-sm">
                    검색 결과가 없어요.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
