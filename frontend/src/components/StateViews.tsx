import { RadarSpinner } from './RadarSpinner';

function Frame({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-[40vh] flex-col items-center justify-center gap-3 text-center">
      {children}
    </div>
  );
}

export function LoadingState({ message = '데이터를 스캔하는 중…' }: { message?: string }) {
  return (
    <Frame>
      <RadarSpinner size={48} />
      <p className="sr-muted text-sm">{message}</p>
    </Frame>
  );
}

export function ErrorState({ onRetry }: { onRetry?: () => void }) {
  return (
    <Frame>
      <p className="text-sm" style={{ color: 'var(--sr-up)' }}>
        데이터를 불러오지 못했어요.
      </p>
      <p className="sr-muted text-sm">백엔드 서버가 실행 중인지 확인해 주세요 (localhost:8080).</p>
      {onRetry && (
        <button className="sr-input mt-1 text-sm" onClick={onRetry}>
          다시 시도
        </button>
      )}
    </Frame>
  );
}

export function EmptyState({ message }: { message: string }) {
  return (
    <Frame>
      <RadarSpinner size={44} />
      <p className="sr-muted text-sm">{message}</p>
    </Frame>
  );
}
