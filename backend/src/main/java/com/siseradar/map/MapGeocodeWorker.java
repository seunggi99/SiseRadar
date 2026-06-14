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
      GeocodeStatus status = GeocodeStatus.FAILED;
      Double lat = null;
      Double lng = null;

      String query = (umdNm == null ? "" : umdNm + " ") + buildingName;
      GeoPlace gp = kakao.geocodePlace(query);
      if (gp != null && inExpectedSigungu(lawdCd, gp.addressName())) {
        status = GeocodeStatus.SUCCESS;
        lat = gp.lat();
        lng = gp.lng();
      }
      repo.save(new ComplexGeocode(lawdCd, pt, buildingName, lat, lng, status));
    } catch (DataIntegrityViolationException dup) {
      // another worker already cached it — fine
    } catch (RuntimeException e) {
      log.warn("Geocode failed {} {} {}: {}", lawdCd, pt, buildingName, e.getMessage());
      try {
        repo.save(new ComplexGeocode(lawdCd, pt, buildingName, null, null, GeocodeStatus.FAILED));
      } catch (RuntimeException ignored) {
        /* row may already exist */
      }
    } finally {
      inFlight.remove(key);
    }
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
