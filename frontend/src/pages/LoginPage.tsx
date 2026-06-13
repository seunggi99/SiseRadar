import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, api } from '../api/client';
import { Header } from '../components/Header';
import { useAuth } from '../lib/auth';

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const isSignup = mode === 'signup';

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const res = isSignup ? await api.signup(email, password) : await api.login(email, password);
      login(res.token, res.email);
      navigate('/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '문제가 발생했어요. 다시 시도해 주세요.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen">
      <Header />
      <main className="mx-auto flex max-w-sm flex-col px-5 py-16">
        <h1 className="mb-1 text-xl font-medium tracking-tight">
          {isSignup ? '회원가입' : '로그인'}
        </h1>
        <p className="sr-muted mb-6 text-sm">
          관심 지역·단지를 등록하면 새 거래를 레이더가 알려드려요.
        </p>

        <form onSubmit={submit} className="flex flex-col gap-3">
          <label className="flex flex-col gap-1.5">
            <span className="sr-label">이메일</span>
            <input
              className="sr-input text-sm"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="sr-label">비밀번호 {isSignup && <span className="opacity-60">(8자 이상)</span>}</span>
            <input
              className="sr-input text-sm"
              type="password"
              autoComplete={isSignup ? 'new-password' : 'current-password'}
              required
              minLength={isSignup ? 8 : undefined}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>

          {error && (
            <p className="text-sm" style={{ color: 'var(--sr-up)' }}>
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={busy}
            className="mt-1 rounded-[8px] px-4 py-2.5 text-sm font-medium disabled:opacity-60"
            style={{ background: 'var(--sr-accent)', color: 'var(--sr-navy)' }}
          >
            {busy ? '처리 중…' : isSignup ? '가입하기' : '로그인'}
          </button>
        </form>

        <button
          className="sr-muted mt-5 text-sm hover:text-[var(--sr-text)]"
          onClick={() => {
            setMode(isSignup ? 'login' : 'signup');
            setError(null);
          }}
        >
          {isSignup ? '이미 계정이 있으신가요? 로그인' : '계정이 없으신가요? 회원가입'}
        </button>
      </main>
    </div>
  );
}
