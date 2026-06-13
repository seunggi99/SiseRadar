import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { NotificationBell } from './NotificationBell';
import { ThemeToggle } from './ThemeToggle';

function navClass({ isActive }: { isActive: boolean }) {
  return `px-1 text-sm transition-colors ${
    isActive ? 'text-[var(--sr-text)]' : 'sr-muted hover:text-[var(--sr-text)]'
  }`;
}

export function Header() {
  const { isAuthenticated, email, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="border-b sr-divide" style={{ borderBottomWidth: '0.5px' }}>
      <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-3.5">
        <div className="flex items-center gap-5">
          <Link to="/" className="flex items-center gap-2.5">
            <img src="/logo-mark.svg" alt="" className="h-7 w-7" />
            <span className="text-lg font-medium tracking-tight">
              <span style={{ color: 'var(--sr-text)' }}>Sise</span>
              <span style={{ color: 'var(--sr-accent)' }}>Radar</span>
            </span>
          </Link>
          <nav className="hidden items-center gap-4 sm:flex">
            <NavLink to="/" end className={navClass}>
              대시보드
            </NavLink>
            <NavLink to="/compare" className={navClass}>
              비교
            </NavLink>
            {isAuthenticated && (
              <NavLink to="/watchlist" className={navClass}>
                관심목록
              </NavLink>
            )}
          </nav>
        </div>

        <div className="flex items-center gap-2.5">
          {isAuthenticated ? (
            <>
              <NotificationBell />
              <button
                onClick={() => {
                  logout();
                  navigate('/');
                }}
                className="sr-muted px-1 text-sm hover:text-[var(--sr-text)]"
                title={email ?? undefined}
              >
                로그아웃
              </button>
            </>
          ) : (
            <Link to="/login" className="sr-muted px-1 text-sm hover:text-[var(--sr-text)]">
              로그인
            </Link>
          )}
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
