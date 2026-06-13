interface KpiCardProps {
  label: string;
  value: string;
  sub?: string;
  /** Override the value color (used for up/down deltas). */
  valueColor?: string;
}

export function KpiCard({ label, value, sub, valueColor }: KpiCardProps) {
  return (
    <div className="sr-card sr-kpi flex flex-col gap-1.5">
      <span className="sr-label">{label}</span>
      <span
        className="sr-num text-2xl font-medium leading-none"
        style={{ color: valueColor ?? 'var(--sr-text)' }}
      >
        {value}
      </span>
      {sub && <span className="sr-num sr-muted text-xs">{sub}</span>}
    </div>
  );
}
