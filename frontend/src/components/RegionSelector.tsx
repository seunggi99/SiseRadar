import { REGIONS } from '../lib/regions';

interface RegionSelectorProps {
  value: string;
  onChange: (lawdCd: string) => void;
}

export function RegionSelector({ value, onChange }: RegionSelectorProps) {
  return (
    <select
      className="sr-input text-sm"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label="지역 선택"
    >
      {REGIONS.map((r) => (
        <option key={r.lawdCd} value={r.lawdCd}>
          {r.name}
        </option>
      ))}
    </select>
  );
}
