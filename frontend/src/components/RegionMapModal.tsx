import { useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import { isInKorea, loadKakao, restrictToKorea } from '../lib/kakaoMap';
import { isKnownRegion } from '../lib/regions';
import { useToast } from '../lib/toast';
import { RadarSpinner } from './RadarSpinner';

interface Props {
  onSelect: (lawdCd: string) => void;
  onClose: () => void;
}

export function RegionMapModal({ onSelect, onClose }: Props) {
  const { toast } = useToast();
  const mapRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState(false);
  const [ready, setReady] = useState(false);
  const [picking, setPicking] = useState<string | null>(null);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  useEffect(() => {
    let cancelled = false;
    loadKakao()
      .then((kakao) => {
        if (cancelled || !mapRef.current) return;
        const map = new kakao.maps.Map(mapRef.current, {
          center: new kakao.maps.LatLng(37.5172, 127.0473), // 강남 일대
          level: 8,
        });
        restrictToKorea(kakao, map);
        setReady(true);
        kakao.maps.event.addListener(map, 'click', async (mouseEvent: any) => {
          const ll = mouseEvent.latLng;
          if (!isInKorea(ll.getLat(), ll.getLng())) {
            toast('국내 지역만 지원해요');
            return;
          }
          setPicking('…');
          try {
            const r = await api.regions.resolve(ll.getLng(), ll.getLat());
            if (!isKnownRegion(r.lawdCd)) {
              setPicking(null);
              toast('국내 지역만 지원해요');
              return;
            }
            onSelect(r.lawdCd);
            onClose();
          } catch {
            setPicking(null);
            toast('국내 지역만 지원해요');
          }
        });
      })
      .catch(() => setError(true));
    return () => {
      cancelled = true;
    };
  }, [onClose, onSelect, toast]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-8"
      style={{ background: 'rgba(0,0,0,0.45)' }}
      onClick={onClose}
    >
      <div
        className="sr-surface flex w-full max-w-2xl flex-col"
        style={{ boxShadow: '0 12px 40px rgba(0,0,0,0.3)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          className="flex items-center justify-between border-b sr-divide px-5 py-3.5"
          style={{ borderBottomWidth: '0.5px' }}
        >
          <div className="flex flex-col">
            <h2 className="text-base font-medium">지도에서 지역 선택</h2>
            <span className="sr-muted text-xs">지도를 클릭하면 해당 시·군·구로 이동해요</span>
          </div>
          <button className="sr-muted text-lg leading-none hover:text-[var(--sr-text)]" onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </div>

        <div className="relative">
          <div ref={mapRef} style={{ width: '100%', height: 420 }} />
          {!ready && !error && (
            <div className="absolute inset-0 flex items-center justify-center" style={{ background: 'var(--sr-surface)' }}>
              <RadarSpinner size={44} />
            </div>
          )}
          {error && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 px-6 text-center" style={{ background: 'var(--sr-surface)' }}>
              <p className="text-sm" style={{ color: 'var(--sr-up)' }}>지도를 불러오지 못했어요.</p>
              <p className="sr-muted text-xs">
                Kakao 개발자 콘솔에서 이 도메인(localhost:5173)이 Web 플랫폼에 등록됐는지 확인해 주세요.
              </p>
            </div>
          )}
          {picking && (
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 sr-surface px-3 py-1.5 text-xs sr-muted">
              선택한 위치를 확인하는 중…
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
