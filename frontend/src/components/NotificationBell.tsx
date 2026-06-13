import { useEffect, useRef, useState } from 'react';
import { useMarkRead, useNotifications, useUnreadCount } from '../api/hooks';

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const unread = useUnreadCount(true);
  const notifications = useNotifications(open);
  const markRead = useMarkRead();

  const count = unread.data?.count ?? 0;

  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((v) => !v)}
        className="sr-surface relative flex h-9 w-9 items-center justify-center"
        aria-label={`알림 ${count}개`}
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.7 21a2 2 0 0 1-3.4 0" />
        </svg>
        {count > 0 && (
          <span
            className="sr-num absolute -right-1.5 -top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[10px] font-medium"
            style={{ background: 'var(--sr-up)', color: '#fff' }}
          >
            {count > 99 ? '99+' : count}
          </span>
        )}
      </button>

      {open && (
        <div
          className="sr-surface absolute right-0 z-50 mt-2 max-h-[400px] w-80 overflow-y-auto"
          style={{ boxShadow: '0 8px 24px rgba(0,0,0,0.18)' }}
        >
          <div className="sr-label border-b sr-divide px-3 py-2.5" style={{ borderBottomWidth: '0.5px' }}>
            알림
          </div>
          {notifications.isLoading ? (
            <p className="sr-muted px-3 py-6 text-center text-sm">불러오는 중…</p>
          ) : !notifications.data?.length ? (
            <p className="sr-muted px-3 py-8 text-center text-sm">새 알림이 없어요.</p>
          ) : (
            notifications.data.map((n) => (
              <button
                key={n.id}
                onClick={() => !n.read && markRead.mutate(n.id)}
                className="block w-full border-b sr-divide px-3 py-2.5 text-left text-sm hover:bg-[var(--sr-surface-2)]"
                style={{ borderBottomWidth: '0.5px', opacity: n.read ? 0.55 : 1 }}
              >
                <div className="flex items-start gap-2">
                  {!n.read && (
                    <span
                      className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full"
                      style={{ background: 'var(--sr-accent)' }}
                    />
                  )}
                  <span className={n.read ? '' : 'ml-0'}>{n.message}</span>
                </div>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
