import { useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../api/client';
import { useMapComplexes, useMapRegions } from '../api/hooks';
import type { MapComplex, MapRegion } from '../api/types';
import { Header } from '../components/Header';
import { PropertyTradeSelector } from '../components/PropertyTradeSelector';
import { RadarSpinner } from '../components/RadarSpinner';
import { RegionSearch } from '../components/RegionSearch';
import { useFilters } from '../lib/filters';
import { isInKorea, loadKakao, restrictToKorea } from '../lib/kakaoMap';
import { colorForArea, scaleThresholds, TEAL_SHADES } from '../lib/priceScale';
import { propertyMeta } from '../lib/propertyTypes';
import { isKnownRegion, regionName } from '../lib/regions';

// Kakao 레벨은 클수록 더 멀리(축소). 이 레벨 이상이면 지역 버블, 미만이면 단지 마커.
const REGION_ZOOM = 8;

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

const pyeong = (v: number) => Math.round(v * 3.3058).toLocaleString('ko-KR');
const sqm = (v: number) => Math.round(v).toLocaleString('ko-KR');

function complexPopupHtml(c: MapComplex): string {
  return `<div class="sr-surface" style="padding:8px 11px;font-size:12px;min-width:160px;transform:translateY(-10px);box-shadow:0 6px 20px rgba(0,0,0,.25)">
    <div style="font-weight:500;margin-bottom:3px;color:var(--sr-text)">${escapeHtml(c.buildingName)}</div>
    <div class="sr-num" style="color:var(--sr-text)">평당(전용) 평균 ${pyeong(c.avgPricePerArea)}만 · 중위 ${pyeong(c.medianPricePerArea)}만</div>
    <div class="sr-num" style="color:var(--sr-text-muted)">㎡당 ${sqm(c.avgPricePerArea)}만 · 거래 ${c.count}건</div>
  </div>`;
}

function regionPopupHtml(r: MapRegion): string {
  return `<div class="sr-surface" style="padding:8px 11px;font-size:12px;min-width:180px;transform:translateY(-14px);box-shadow:0 6px 20px rgba(0,0,0,.25)">
    <div style="font-weight:500;margin-bottom:3px;color:var(--sr-text)">${escapeHtml(regionName(r.lawdCd))}</div>
    <div class="sr-num" style="color:var(--sr-text)">평당(전용) 평균 ${pyeong(r.avgPricePerArea)}만 · 중위 ${pyeong(r.medianPricePerArea)}만</div>
    <div class="sr-num" style="color:var(--sr-text-muted)">㎡당 ${sqm(r.avgPricePerArea)}만 · 전체 거래 ${r.count.toLocaleString('ko-KR')}건</div>
  </div>`;
}

/** 버블 지름 — 거래량 기반(√ 스케일), 절대 색은 colorForArea가 결정. */
function bubbleDiameter(count: number): number {
  return Math.max(26, Math.min(64, Math.round(22 + Math.sqrt(count) * 0.9)));
}

function bubbleElement(r: MapRegion, color: string, onClick: () => void): HTMLElement {
  const d = bubbleDiameter(r.count);
  const el = document.createElement('div');
  el.style.cssText = `width:${d}px;height:${d}px;line-height:${d}px;border-radius:50%;background:${color};color:#06241f;font-size:11px;font-weight:700;text-align:center;border:1.5px solid rgba(255,255,255,0.85);box-shadow:0 2px 8px rgba(0,0,0,.25);cursor:pointer;font-variant-numeric:tabular-nums;`;
  el.textContent = r.count >= 1000 ? `${Math.round(r.count / 100) / 10}k` : String(r.count);
  el.title = regionName(r.lawdCd);
  el.addEventListener('click', (e) => {
    e.stopPropagation();
    onClick();
  });
  return el;
}

export function MapPage() {
  const { lawdCd, propertyType, tradeType, from, to, band, setLawdCd, setProperty, setTradeType } =
    useFilters();
  const mapRef = useRef<HTMLDivElement>(null);
  const kakaoRef = useRef<any>(null);
  const mapObj = useRef<any>(null);
  const clusterer = useRef<any>(null);
  const overlay = useRef<any>(null);
  const bubbleOverlays = useRef<any[]>([]);
  const fittedKey = useRef<string>('');
  const [ready, setReady] = useState(false);
  const [error, setError] = useState(false);
  const [level, setLevel] = useState(6);

  const showBubbles = level >= REGION_ZOOM;
  const hasMarkers = propertyMeta(propertyType).hasRanking; // 건물명 유형만 마커

  const complexes = useMapComplexes(lawdCd, propertyType, tradeType, from, to, band ?? undefined);
  const regions = useMapRegions(propertyType, tradeType, from, to, band ?? undefined);
  const markerData = useMemo(
    () => (hasMarkers ? (complexes.data ?? []) : []),
    [complexes.data, hasMarkers],
  );
  const regionData = regions.data ?? [];

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
        setLevel(map.getLevel());
        setReady(true);
        kakao.maps.event.addListener(map, 'zoom_changed', () => setLevel(map.getLevel()));
        kakao.maps.event.addListener(map, 'click', async (e: any) => {
          overlay.current?.setMap(null);
          const ll = e.latLng;
          // 지도 탐색 클릭: 유효한 남한 시군구로 해석될 때만 이동. 그 외(강·공원·미해석·
          // 바다·국외)는 조용히 무시 — 안내 토스트는 검색/모달의 명시적 선택에서만.
          if (!isInKorea(ll.getLat(), ll.getLng())) return;
          try {
            const r = await api.regions.resolve(ll.getLng(), ll.getLat());
            if (isKnownRegion(r.lawdCd)) setLawdCd(r.lawdCd);
          } catch {
            /* 미해석·비마스터·국외 → no-op */
          }
        });
      })
      .catch(() => setError(true));
    return () => {
      cancelled = true;
    };
  }, [setLawdCd]);

  // 단지 마커: 고줌(showBubbles=false)일 때만. 색은 고정 절대 스케일.
  useEffect(() => {
    const kakao = kakaoRef.current;
    if (!ready || !kakao || !clusterer.current || !mapObj.current) return;

    clusterer.current.clear();
    if (showBubbles) return; // 저줌 → 마커 숨김(버블 모드)

    overlay.current?.setMap(null);
    const markers = markerData.map((c) => {
      const marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(c.lat, c.lng),
        image: markerImage(kakao, colorForArea(c.avgPricePerArea, tradeType)),
        title: c.buildingName,
      });
      kakao.maps.event.addListener(marker, 'click', () => {
        overlay.current?.setMap(null);
        overlay.current = new kakao.maps.CustomOverlay({
          position: marker.getPosition(),
          content: complexPopupHtml(c),
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
    if (markerData.length > 0 && fittedKey.current !== key) {
      const bounds = new kakao.maps.LatLngBounds();
      markerData.forEach((c) => bounds.extend(new kakao.maps.LatLng(c.lat, c.lng)));
      mapObj.current.setBounds(bounds);
      fittedKey.current = key;
    }
  }, [markerData, ready, showBubbles, tradeType, lawdCd, propertyType]);

  // 지역 버블: 저줌(showBubbles)일 때만. 마커와 같은 절대 색 스케일 공유.
  useEffect(() => {
    const kakao = kakaoRef.current;
    if (!ready || !kakao || !mapObj.current) return;

    bubbleOverlays.current.forEach((o) => o.setMap(null));
    bubbleOverlays.current = [];
    if (!showBubbles) return;

    overlay.current?.setMap(null);
    bubbleOverlays.current = regionData.map((r) => {
      const el = bubbleElement(r, colorForArea(r.avgPricePerArea, tradeType), () => {
        overlay.current?.setMap(null);
        overlay.current = new kakao.maps.CustomOverlay({
          position: new kakao.maps.LatLng(r.lat, r.lng),
          content: regionPopupHtml(r),
          yAnchor: 1,
          zIndex: 100,
        });
        overlay.current.setMap(mapObj.current);
      });
      const ov = new kakao.maps.CustomOverlay({
        position: new kakao.maps.LatLng(r.lat, r.lng),
        content: el,
        yAnchor: 0.5,
        xAnchor: 0.5,
        zIndex: 50,
      });
      ov.setMap(mapObj.current);
      return ov;
    });
  }, [regionData, ready, showBubbles, tradeType]);

  const thresholds = scaleThresholds(tradeType);
  const unitLabel = tradeType === 'RENT' ? '보증금 ㎡당' : '평당가(전용)';

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

          {/* color legend — 마커·버블 공유 고정 절대 스케일 (₩/㎡ 구간) */}
          {ready && (
            <div className="sr-surface absolute left-3 top-3 flex flex-col gap-1.5 px-3 py-2 text-xs" style={{ zIndex: 5 }}>
              <span className="sr-muted">
                {unitLabel} · 만원/㎡ {showBubbles ? `· ${regionData.length}지역` : ''}
              </span>
              <div className="flex items-end gap-0">
                {TEAL_SHADES.map((s, i) => (
                  <div key={s} className="flex flex-col items-center" style={{ width: 34 }}>
                    <span className="h-3 w-full" style={{ background: s }} />
                    <span className="sr-num sr-muted mt-0.5" style={{ fontSize: 10 }}>
                      {i < thresholds.length ? thresholds[i].toLocaleString('ko-KR') : ''}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* zoom hint */}
          {ready && (
            <div className="sr-surface absolute right-3 top-3 px-2.5 py-1 text-xs sr-muted" style={{ zIndex: 5 }}>
              {showBubbles ? '지역 버블 — 확대하면 단지' : '단지 마커 — 축소하면 지역'}
            </div>
          )}

          {!hasMarkers && ready && !showBubbles && (
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 sr-surface px-3 py-1.5 text-xs sr-muted">
              이 유형은 개별 단지 위치를 제공하지 않아요 — 축소하면 지역 집계가 보여요
            </div>
          )}
          {hasMarkers && ready && !showBubbles && markerData.length === 0 && (
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 sr-surface px-3 py-1.5 text-xs sr-muted">
              <RadarSpinner size={14} /> 단지 위치를 불러오는 중…
            </div>
          )}
          {showBubbles && ready && regionData.length === 0 && (
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 sr-surface px-3 py-1.5 text-xs sr-muted">
              집계된 지역이 아직 없어요 (단지 지오코딩 후 채워져요)
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
