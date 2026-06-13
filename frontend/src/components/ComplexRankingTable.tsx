import { useMemo, useState } from 'react';
import type { ComplexRank } from '../api/types';
import { formatCount, formatEok, formatPyeongPrice } from '../lib/format';

type SortKey = 'rank' | 'avgAmount' | 'count' | 'avgPricePerPyeong' | 'maxAmount';

interface ComplexRankingTableProps {
  rows: ComplexRank[];
  /** When provided, each row shows a "관심 추가" button calling this with the complex name. */
  onAddComplex?: (aptName: string) => void;
  /** When provided, the complex name becomes a button opening the detail view. */
  onSelectComplex?: (aptName: string) => void;
}

const COLUMNS: { key: SortKey; label: string; numeric: boolean }[] = [
  { key: 'rank', label: '순위', numeric: false },
  { key: 'avgAmount', label: '평균가', numeric: true },
  { key: 'maxAmount', label: '최고가', numeric: true },
  { key: 'avgPricePerPyeong', label: '평당가', numeric: true },
  { key: 'count', label: '거래량', numeric: true },
];

export function ComplexRankingTable({
  rows,
  onAddComplex,
  onSelectComplex,
}: ComplexRankingTableProps) {
  const [query, setQuery] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('rank');
  const [asc, setAsc] = useState(true);

  const filtered = useMemo(() => {
    const q = query.trim();
    const base = q ? rows.filter((r) => r.aptName.includes(q)) : rows;
    const sorted = [...base].sort((a, b) => {
      const diff = a[sortKey] - b[sortKey];
      return asc ? diff : -diff;
    });
    return sorted;
  }, [rows, query, sortKey, asc]);

  function toggleSort(key: SortKey) {
    if (key === sortKey) {
      setAsc((v) => !v);
    } else {
      setSortKey(key);
      // ranks read best ascending; metrics most useful descending
      setAsc(key === 'rank');
    }
  }

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
                {COLUMNS.filter((col) => col.key !== 'rank').map((col) => (
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
                <tr
                  key={r.aptName}
                  className="border-t sr-divide"
                  style={{ borderTopWidth: '0.5px' }}
                >
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
                  <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right">
                    {formatEok(r.avgAmount)}
                  </td>
                  <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right sr-muted">
                    {formatEok(r.maxAmount)}
                  </td>
                  <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right">
                    {formatPyeongPrice(r.avgPricePerPyeong)}
                  </td>
                  <td className="sr-num whitespace-nowrap px-3 py-2.5 text-right sr-muted">
                    {formatCount(r.count)}
                  </td>
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
                  <td colSpan={onAddComplex ? 6 : 5} className="sr-muted px-3 py-8 text-center text-sm">
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
