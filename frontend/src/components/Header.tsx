import { ThemeToggle } from './ThemeToggle';

export function Header() {
  return (
    <header className="border-b sr-divide" style={{ borderBottomWidth: '0.5px' }}>
      <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-3.5">
        <div className="flex items-center gap-2.5">
          <img src="/logo-mark.svg" alt="" className="h-7 w-7" />
          <span className="text-lg font-medium tracking-tight">
            <span style={{ color: 'var(--sr-text)' }}>Sise</span>
            <span style={{ color: 'var(--sr-accent)' }}>Radar</span>
          </span>
        </div>
        <ThemeToggle />
      </div>
    </header>
  );
}
