// Brand palette (BRAND.md). Up = red, Down = blue (Korean convention); teal is brand only.
export const BRAND = {
  teal: '#14C2B2',
  tealBright: '#39E0CE',
  tealDeep: '#0B8276',
  tealTint: '#BDEDE7',
  up: '#E5484D',
  down: '#2F6FED',
  slate: '#5B6B7F',
} as const;

/**
 * Categorical palette for the compare view (one color per region). Kept to 3, teal-led, and
 * deliberately distinct from the up-red/down-blue direction colors so meaning stays unambiguous.
 */
export const COMPARE_COLORS = ['#14C2B2', '#8B7FD6', '#E0A93B'] as const;

/** Direction color for a delta: up = red, down = blue, flat = muted. */
export function directionColor(value: number | null | undefined): string {
  if (value === null || value === undefined || value === 0) return 'var(--sr-text-muted)';
  return value > 0 ? BRAND.up : BRAND.down;
}

/** Recharts colors that adapt to the current theme. */
export function chartColors(isDark: boolean) {
  return {
    axis: isDark ? '#9FB1C6' : '#5B6B7F',
    grid: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(14,27,46,0.08)',
    line: isDark ? BRAND.tealBright : BRAND.tealDeep,
    bar: isDark ? 'rgba(57,224,206,0.22)' : 'rgba(20,194,178,0.20)',
    tooltipBg: isDark ? '#13243B' : '#FFFFFF',
    tooltipBorder: isDark ? 'rgba(255,255,255,0.12)' : 'rgba(14,27,46,0.12)',
  };
}
