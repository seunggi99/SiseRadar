// Shared Kakao Maps JS SDK loader + Korea-only restriction. Used by the map tab and the picker modal.

declare global {
  interface Window {
    kakao: any;
  }
}

const KAKAO_KEY = import.meta.env.VITE_KAKAO_MAP_KEY;

/** Loads the Kakao Maps SDK once (autoload=false) and resolves when ready. */
export function loadKakao(): Promise<any> {
  if (window.kakao?.maps) return Promise.resolve(window.kakao);
  return new Promise((resolve, reject) => {
    const finish = () => window.kakao.maps.load(() => resolve(window.kakao));
    const existing = document.getElementById('kakao-sdk') as HTMLScriptElement | null;
    if (existing) {
      existing.addEventListener('load', finish);
      existing.addEventListener('error', reject);
      return;
    }
    const s = document.createElement('script');
    s.id = 'kakao-sdk';
    s.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_KEY}&autoload=false`;
    s.onload = finish;
    s.onerror = reject;
    document.head.appendChild(s);
  });
}

/** Rough bounding box of South Korea (lat/lng). */
export const KOREA_BOUNDS = {
  minLat: 33.0,
  maxLat: 38.7,
  minLng: 124.5,
  maxLng: 132.0,
};

/**
 * Keeps the map within Korea: clamps the center back inside KOREA_BOUNDS whenever it drifts out
 * (pan/zoom), and caps zoom-out. Prevents selecting/viewing outside the country.
 */
export function restrictToKorea(kakao: any, map: any) {
  map.setMaxLevel(13); // don't zoom out past the peninsula
  const clamp = () => {
    const c = map.getCenter();
    const lat = Math.min(Math.max(c.getLat(), KOREA_BOUNDS.minLat), KOREA_BOUNDS.maxLat);
    const lng = Math.min(Math.max(c.getLng(), KOREA_BOUNDS.minLng), KOREA_BOUNDS.maxLng);
    if (lat !== c.getLat() || lng !== c.getLng()) {
      map.setCenter(new kakao.maps.LatLng(lat, lng));
    }
  };
  kakao.maps.event.addListener(map, 'dragend', clamp);
  kakao.maps.event.addListener(map, 'zoom_changed', clamp);
}

/** True if a coordinate is inside Korea's bounding box. */
export function isInKorea(lat: number, lng: number): boolean {
  return (
    lat >= KOREA_BOUNDS.minLat &&
    lat <= KOREA_BOUNDS.maxLat &&
    lng >= KOREA_BOUNDS.minLng &&
    lng <= KOREA_BOUNDS.maxLng
  );
}
