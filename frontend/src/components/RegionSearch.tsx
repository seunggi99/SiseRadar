import { useEffect, useMemo, useRef, useState } from 'react';
import type { Region } from '../lib/regions';
import { searchRegions } from '../lib/regions';

interface RegionSearchProps {
  onSelect: (region: Region) => void;
  placeholder?: string;
}

/** Autocomplete over all 250 시군구 — instant, client-side (no API). */
export function RegionSearch({ onSelect, placeholder = '지역 검색 (예: 강남구)' }: RegionSearchProps) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const ref = useRef<HTMLDivElement>(null);

  const matches = useMemo(() => searchRegions(query, 20), [query]);

  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  function choose(r: Region) {
    onSelect(r);
    setQuery('');
    setOpen(false);
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (!open || !matches.length) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((a) => Math.min(a + 1, matches.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((a) => Math.max(a - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      choose(matches[active]);
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  }

  return (
    <div className="relative" ref={ref}>
      <input
        className="sr-input w-56 text-sm"
        value={query}
        placeholder={placeholder}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
          setActive(0);
        }}
        onFocus={() => query && setOpen(true)}
        onKeyDown={onKeyDown}
      />
      {open && matches.length > 0 && (
        <div
          className="sr-surface absolute right-0 z-50 mt-1 max-h-72 w-64 overflow-y-auto"
          style={{ boxShadow: '0 8px 24px rgba(0,0,0,0.18)' }}
        >
          {matches.map((r, i) => (
            <button
              key={r.lawdCd}
              onMouseEnter={() => setActive(i)}
              onClick={() => choose(r)}
              className="flex w-full items-center justify-between px-3 py-2 text-left text-sm"
              style={{ background: i === active ? 'var(--sr-surface-2)' : 'transparent' }}
            >
              <span>{r.name}</span>
              <span className="sr-num sr-muted text-xs">{r.lawdCd}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
