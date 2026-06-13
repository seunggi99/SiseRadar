import { createContext, useCallback, useContext, useRef, useState } from 'react';
import type { ReactNode } from 'react';

interface Toast {
  id: number;
  message: string;
  tone: 'default' | 'error';
}

interface ToastContextValue {
  toast: (message: string, tone?: 'default' | 'error') => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(1);

  const toast = useCallback((message: string, tone: 'default' | 'error' = 'default') => {
    const id = nextId.current++;
    setToasts((cur) => [...cur, { id, message, tone }]);
    setTimeout(() => setToasts((cur) => cur.filter((t) => t.id !== id)), 2600);
  }, []);

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-6 z-50 flex flex-col items-center gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            className="sr-surface px-4 py-2 text-sm shadow-sm"
            style={{ color: t.tone === 'error' ? 'var(--sr-up)' : 'var(--sr-text)' }}
          >
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
