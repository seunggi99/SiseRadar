import { useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../api/client';
import { useMapComplexes } from '../api/hooks';
import type { MapComplex } from '../api/types';
import { Header } from '../components/Header';
import { PropertyTradeSelector } from '../components/PropertyTradeSelector';
import { RadarSpinner } from '../components/RadarSpinner';
import { RegionSearch } from '../components/RegionSearch';
import { useFilters } from '../lib/filters';
import { isInKorea, loadKakao, restrictToKorea } from '../lib/kakaoMap';
import { propertyMeta } from '../lib/propertyTypes';
import { isKnownRegion, regionName } from '../lib/regions';
import { useToast } from '../lib/toast';

// teal sequential scale (light=low price → dark=high price). Never red/blue (those = up/down only).
const TEAL_SHADES = ['#CDEFEA', '#8FD9CE', '#39C9B9', '#0E9E8F', '#0B5F56'];

function makeColorScale(values: number[]): (v: number) => string {
  if (values.length === 0) return () => TEAL_SHADES[2];
  const sorted = [...values].sort((a, b) => a - b);
  const q = (p: number) => sorted[Math.min(sorted.length - 1, Math.floor((sorted.length - 1) * p))];
  const th = [q(0.2), q(0.4), q(0.6), q(0.8)];
  return (v: number) => {
    for (let i = 0; i < th.length; i++) if (v <= th[i]) return TEAL_SHADES[i];
    return TEAL_SHADES[4];
  };
}

function markerImage(kakao: any, color: string) {
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='18' height='18'><circle cx='9' cy='9' r='6.5' fill='${color}' stroke='white' stroke-opacity='0.9' stroke-width='1.5'/></svg>`;
  return new kakao.maps.MarkerImage(
    'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg),
    new kakao.maps.Size(18, 18),
    { offset: new kakao.maps.Point(9, 9) },
  );
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]!);
}

function popupHtml(c: MapComplex): string {
  const pyeong = (v: number) => Math.round(v * 3.3058).toLocaleString('ko-KR');
  return `<div class="sr-surface" style="padding:8px 11px;font-size:12px;min-width:160px;transform:translateY(-10px);box-shadow:0 6px 20px rgba(0,0,0,.25)">
    <div style="font-weight:500;margin-bottom:3px;color:var(--sr-text)">${escapeHtml(c.buildingName)}</div>
    <div class="sr-num" style="color:var(--sr-text)">평당(전용) 평균 ${pyeong(c.avgPricePerArea)}만 · 중위 ${pyeong(c.medianPricePerArea)}만</div>
    <div class="sr-num" style="color:var(--sr-text-muted)">㎡당 ${Math.round(c.avgPricePerArea).toLocaleString('ko-KR')}만 · 거래 ${c.count}건</div>
  </div>`;
}

export function MapPage() {
  const { lawdCd, propertyType, tradeType, from, to, band, setLawdCd, setProperty, setTradeType } =
    useFilters();
  const { toast } = useToast();
  const mapRef = useRef<HTMLDivElement>(null);
  const kakaoRef = useRef<any>(null);
  const mapObj = useRef<any>(null);
  const clusterer = useRef<any>(null);
  const overlay = useRef<any>(null);
  const fittedKey = useRef<string>('');
  const [ready, setReady] = useState(false);
  const [error, setError] = useState(false);

  const hasMarkers = propertyMeta(propertyType).hasRanking; // 건물명 유형만 마커
  const complexes = useMapComplexes(
    lawdCd,
    propertyType,
    tradeType,
    from,
    to,
    band ?? undefined,
  );
  const data = useMemo(() => (hasMarkers ? (complexes.data ?? []) : []), [complexes.data, hasMarkers]);

  // init map once
  useEffect(() => {
    let cancelled = false;
    loadKakao()
      .then((kakao) => {
        if (cancelled || !mapRef.current) return;
        kakaoRef.current = kakao;
        const map = new kakao.maps.Map(mapRef.current, {
          center: new kakao.maps.LatLng(37.5665, 126.978),
          level: 6,
        });
        restrictToKorea(kakao, map);
        clusterer.current = new kakao.maps.MarkerClusterer({
          map,
          averageCenter: true,
          minLevel: 5,
          styles: [
            {
              width: '38px',
              height: '38px',
              background: 'rgba(20,194,178,0.85)',
              color: '#06241f',
              borderRadius: '19px',
              textAlign: 'center',
              lineHeight: '38px',
              fontSize: '12px',
              fontWeight: '600',
            },
          ],
        });
        mapObj.current = map;
        setReady(true);
        kakao.maps.event.addListener(map, 'click', async (e: any) => {
          overlay.current?.setMap(null);
          const ll = e.latLng;
          if (!isInKorea(ll.getLat(), ll.getLng())) {
            toast('국내 지역만 지원해요');
            return;
          }
          try {
            const r = await api.regions.resolve(ll.getLng(), ll.getLat());
            if (!isKnownRegion(r.lawdCd)) {
              toast('국내 지역만 지원해요');
              return;
            }
            setLawdCd(r.lawdCd);
          } catch {
            toast('국내 지역만 지원해요');
          }
        });
      })
      .catch(() => setError(true));
    return () => {
      cancelled = true;
    };
  }, [setLawdCd, toast]);

  const colorScale = useMemo(() => makeColorScale(data.map((d) => d.avgPricePerArea)), [data]);

  // (re)build markers when data changes
  useEffect(() => {
    const kakao = kakaoRef.current;
    if (!ready || !kakao || !clusterer.current || !mapObj.current) return;

    overlay.current?.setMap(null);
    clusterer.current.clear();

    const markers = data.map((c) => {
      const marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(c.lat, c.lng),
        image: markerImage(kakao, colorScale(c.avgPricePerArea)),
        title: c.buildingName,
      });
      kakao.maps.event.addListener(marker, 'click', () => {
        overlay.current?.setMap(null);
        overlay.current = new kakao.maps.CustomOverlay({
          position: marker.getPosition(),
          content: popupHtml(c),
          yAnchor: 1,
          zIndex: 100,
        });
        overlay.current.setMap(mapObj.current);
      });
      return marker;
    });
    clusterer.current.addMarkers(markers);

    // fit bounds once per region/filter change (not on every geocode-fill poll)
    const key = `${lawdCd}:${propertyType}:${tradeType}`;
    if (data.length > 0 && fittedKey.current !== key) {
      const bounds = new kakao.maps.LatLngBounds();
      data.forEach((c) => bounds.extend(new kakao.maps.LatLng(c.lat, c.lng)));
      mapObj.current.setBounds(bounds);
      fittedKey.current = key;
    }
  }, [data, ready, colorScale, lawdCd, propertyType, tradeType]);

  const filling = hasMarkers && complexes.isFetched && data.length > 0;

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

          {/* color legend */}
          {hasMarkers && (
            <div className="sr-surface absolute left-3 top-3 flex flex-col gap-1.5 px-3 py-2 text-xs" style={{ zIndex: 5 }}>
              <span className="sr-muted">평당가(전용) {filling ? `· ${data.length}단지` : ''}</span>
              <div className="flex items-center gap-1">
                <span className="sr-muted">낮음</span>
                {TEAL_SHADES.map((s) => (
                  <span key={s} className="h-3 w-4 rounded-sm" style={{ background: s }} />
                ))}
                <span className="sr-muted">높음</span>
              </div>
            </div>
          )}

          {!hasMarkers && ready && (
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 sr-surface px-3 py-1.5 text-xs sr-muted">
              이 유형은 개별 단지 위치를 제공하지 않아요 (지역 집계는 곧 추가)
            </div>
          )}
          {hasMarkers && ready && data.length === 0 && (
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 sr-surface px-3 py-1.5 text-xs sr-muted">
              <RadarSpinner size={14} /> 단지 위치를 불러오는 중…
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
