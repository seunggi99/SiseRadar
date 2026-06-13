/**
 * Placeholder landing screen — verifies the design tokens, Tailwind, fonts and
 * logo wire up correctly. Real screens arrive in Phase 2.
 */
function App() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 px-6 text-center">
      <img src="/logo-mark.svg" alt="SiseRadar" className="h-20 w-20" />
      <h1 className="text-2xl font-medium tracking-tight">
        <span style={{ color: 'var(--sr-text)' }}>Sise</span>
        <span style={{ color: 'var(--sr-accent)' }}>Radar</span>
      </h1>
      <p className="max-w-sm text-sm" style={{ color: 'var(--sr-text-muted)' }}>
        시장을 스캔해서 좋은 거래를 포착하는 레이더.
      </p>
      <code className="sr-num text-xs" style={{ color: 'var(--sr-text-muted)' }}>
        API: {import.meta.env.VITE_API_BASE_URL ?? 'unset'}
      </code>
    </div>
  );
}

export default App;
