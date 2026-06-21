package com.siseradar.map;

import com.siseradar.collect.KakaoClient;
import com.siseradar.collect.KakaoClient.GeoPlace;
import com.siseradar.collect.KoreaRegions;
import com.siseradar.domain.ComplexGeocode;
import com.siseradar.domain.GeocodeStatus;
import com.siseradar.domain.PropertyType;
import com.siseradar.repository.ComplexGeocodeRepository;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Forward-geocodes one building in the background and caches the result — in a single Kakao call.
 * The geocoded point is accepted (SUCCESS) only if the returned address actually falls in the
 * expected 시군구 (its address contains the region's 구/시/군 token) — guards against same-name
 * complexes in another 구 without a second reverse-geocode call. In-flight dedup via {@link #claim}.
 */
@Component
public class MapGeocodeWorker {

  private static final Logger log = LoggerFactory.getLogger(MapGeocodeWorker.class);

  private final KakaoClient kakao;
  private final ComplexGeocodeRepository repo;
  private final KoreaRegions koreaRegions;
  private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

  public MapGeocodeWorker(
      KakaoClient kakao, ComplexGeocodeRepository repo, KoreaRegions koreaRegions) {
    this.kakao = kakao;
    this.repo = repo;
    this.koreaRegions = koreaRegions;
  }

  public static String key(String lawdCd, PropertyType pt, String buildingName) {
    return lawdCd + "|" + pt + "|" + buildingName;
  }

  /** Atomically reserve a building for geocoding; false if already in flight. */
  public boolean claim(String key) {
    return inFlight.add(key);
  }

  @Async
  @Transactional
  public void geocode(
      String lawdCd, PropertyType pt, String buildingName, String umdNm, String key) {
    try {
      // lazy 경로: 지번 없이 단지명 키워드만(기존 동작). 주소 폴백은 지번 보유한 warm 경로에서.
      doGeocode(lawdCd, pt, buildingName, umdNm, null, false);
    } catch (DataIntegrityViolationException dup) {
      // another worker already cached it — fine
    } catch (RuntimeException e) {
      // transient (quota/network/unexpected) → leave un-cached so it retries later
      log.warn("Geocode transient error {} {} {}: {} — will retry", lawdCd, pt, buildingName, e.getMessage());
    } finally {
      inFlight.remove(key);
    }
  }

  /**
   * Synchronous warm: geocode one building now, in its own transaction. Returns true if a usable
   * coordinate was cached. {@code jibun} enables the 주소 폴백; {@code addressOnly}=true skips the
   * 단지명 키워드 1차(이미 FAILED인 단지 재시도용 — 주소 검색만). Transient errors propagate so the
   * warm job can back off.
   */
  @Transactional
  public boolean geocodeSync(
      String lawdCd,
      PropertyType pt,
      String buildingName,
      String umdNm,
      String jibun,
      boolean addressOnly) {
    try {
      return doGeocode(lawdCd, pt, buildingName, umdNm, jibun, addressOnly);
    } catch (DataIntegrityViolationException dup) {
      return true; // already cached by a concurrent worker
    }
  }

  /**
   * 2단계 지오코딩 + 캐시 upsert.
   * <ul>
   *   <li>1차(addressOnly=false): 단지명 키워드 검색(기존 로직 그대로).
   *   <li>2차 폴백: 1차가 무결과/시군구 불일치면 "시군구+법정동+지번" 주소 검색. 철거·개명 단지도
   *       필지(지번)는 살아있어 위치가 잡힘.
   * </ul>
   * 시군구 검증(inExpectedSigungu)은 두 결과 모두에 동일 적용. 둘 다 실패면 FAILED. 기존 캐시
   * 행이 있으면 in-place update(재워밍). transient는 propagate(미캐시 → 나중 재시도).
   */
  private boolean doGeocode(
      String lawdCd, PropertyType pt, String buildingName, String umdNm, String jibun, boolean addressOnly) {
    GeoPlace gp = null;
    if (!addressOnly) {
      String query = (umdNm == null ? "" : umdNm + " ") + buildingName;
      GeoPlace kw = kakao.geocodePlace(query); // RestClientException on quota/network → propagate
      if (kw != null && inExpectedSigungu(lawdCd, kw.addressName())) {
        gp = kw;
      }
    }
    if (gp == null && umdNm != null && jibun != null && !jibun.isBlank()) {
      String addr = koreaRegions.sigunguToken(lawdCd) + " " + umdNm + " " + jibun;
      GeoPlace ad = kakao.geocodeAddressPlace(addr);
      if (ad != null && inExpectedSigungu(lawdCd, ad.addressName())) {
        gp = ad;
      }
    }
    boolean ok = gp != null;
    upsert(
        lawdCd, pt, buildingName,
        ok ? gp.lat() : null, ok ? gp.lng() : null,
        ok ? GeocodeStatus.SUCCESS : GeocodeStatus.FAILED);
    return ok;
  }

  /** Upsert the geocode cache row — update in place if it exists(재워밍), else insert. */
  private void upsert(
      String lawdCd, PropertyType pt, String buildingName, Double lat, Double lng, GeocodeStatus status) {
    ComplexGeocode row =
        repo.findByLawdCdAndPropertyTypeAndBuildingName(lawdCd, pt, buildingName).orElse(null);
    if (row != null) {
      row.refresh(lat, lng, status);
    } else {
      row = new ComplexGeocode(lawdCd, pt, buildingName, lat, lng, status);
    }
    repo.save(row);
  }

  /** True if the geocoded address contains the region's 구/시/군 token (single-call validation). */
  private boolean inExpectedSigungu(String lawdCd, String addressName) {
    if (addressName == null) {
      return false;
    }
    String token = koreaRegions.sigunguToken(lawdCd);
    return token != null && addressName.contains(token);
  }
}
