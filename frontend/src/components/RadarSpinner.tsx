import { BRAND } from '../lib/colors';

/**
 * The radar sweep — SiseRadar's signature flourish, reused as the loading indicator.
 * Concentric rings stay still; the teal sweep rotates (frozen under prefers-reduced-motion).
 */
export function RadarSpinner({ size = 40 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 48 48"
      role="status"
      aria-label="불러오는 중"
    >
      <defs>
        <linearGradient id="sr-sweep-grad" x1="24" y1="24" x2="24" y2="2" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor={BRAND.teal} stopOpacity="0.5" />
          <stop offset="100%" stopColor={BRAND.teal} stopOpacity="0" />
        </linearGradient>
      </defs>
      <circle cx="24" cy="24" r="21" fill="none" stroke={BRAND.teal} strokeOpacity="0.18" />
      <circle cx="24" cy="24" r="13" fill="none" stroke={BRAND.teal} strokeOpacity="0.18" />
      <circle cx="24" cy="24" r="5" fill="none" stroke={BRAND.teal} strokeOpacity="0.18" />
      <g className="sr-radar__sweep">
        <path d="M24 24 L24 2 A22 22 0 0 1 43 14 Z" fill="url(#sr-sweep-grad)" />
        <line x1="24" y1="24" x2="24" y2="2" stroke={BRAND.teal} strokeWidth="1.5" />
      </g>
      <circle cx="24" cy="24" r="2" fill={BRAND.teal} />
    </svg>
  );
}
