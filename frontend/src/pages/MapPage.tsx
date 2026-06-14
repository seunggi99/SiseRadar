import { useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import { Header } from '../components/Header';
import { PropertyTradeSelector } from '../components/PropertyTradeSelector';
import { RadarSpinner } from '../components/RadarSpinner';
import { RegionSearch } from '../components/RegionSearch';
import { useFilters } from '../lib/filters';
import { isInKorea, loadKakao, restrictToKorea } from '../lib/kakaoMap';
import { regionName } from '../lib/regions';

/**
 * Map tab — skeleton (increment 1): renders a Korea-restricted Kakao map, reflects the shared
 * filter (지역/건물유형/거래유형), and lets a click pick a region (reused resolve). 시세 마커/버블은
 * 다음 증분에서.
 */
export function MapPage() {
  const { lawdCd, propertyType, tradeType, setLawdCd, setProperty, setTradeType } = useFilters();
  const mapRef = useRef<HTMLDivElement>(null);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    loadKakao()
      .then((kakao) => {
        if (cancelled || !mapRef.current) return;
        const map = new kakao.maps.Map(mapRef.current, {
          center: new kakao.maps.LatLng(37.5665, 126.978), // 서울 시청 일대 (기본)
          level: 6,
        });
        restrictToKorea(kakao, map);
        setReady(true);
        kakao.maps.event.addListener(map, 'click', async (e: any) => {
          const ll = e.latLng;
          if (!isInKorea(ll.getLat(), ll.getLng())) return;
          try {
            const r = await api.regions.resolve(ll.getLng(), ll.getLat());
            setLawdCd(r.lawdCd);
          } catch {
            /* 국외/행정구역 없음 → 무시 */
          }
        });
      })
      .catch(() => setError(true));
    return () => {
      cancelled = true;
    };
  }, [setLawdCd]);

  return (
    <div className="min-h-screen">
      <Header />
      <main className="mx-auto max-w-6xl px-5 py-6">
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
          <div className="flex flex-col gap-1">
            <span className="sr-label">지도</span>
            <h1 className="text-xl font-medium tracking-tight">{regionName(lawdCd)}</h1>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <PropertyTradeSelector
              propertyType={propertyType}
              tradeType={tradeType}
              onPropertyChange={setProperty}
              onTradeChange={setTradeType}
            />
            <RegionSearch onSelect={(r) => setLawdCd(r.lawdCd)} />
          </div>
        </div>

        <div className="sr-surface relative overflow-hidden" style={{ borderRadius: 'var(--sr-radius)' }}>
          <div ref={mapRef} style={{ width: '100%', height: '70vh' }} />
          {!ready && !error && (
            <div className="absolute inset-0 flex items-center justify-center" style={{ background: 'var(--sr-surface)' }}>
              <RadarSpinner size={48} />
            </div>
          )}
          {error && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 px-6 text-center" style={{ background: 'var(--sr-surface)' }}>
              <p className="text-sm" style={{ color: 'var(--sr-up)' }}>지도를 불러오지 못했어요.</p>
              <p className="sr-muted text-xs">Kakao 콘솔에 이 도메인이 Web 플랫폼으로 등록됐는지 확인해 주세요.</p>
            </div>
          )}
          {ready && (
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 sr-surface px-3 py-1.5 text-xs sr-muted">
              지도를 클릭하면 해당 지역으로 이동 · 시세 시각화는 곧 추가됩니다
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
