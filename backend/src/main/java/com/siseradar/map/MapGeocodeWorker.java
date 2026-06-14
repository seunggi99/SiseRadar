package com.siseradar.map;

import com.siseradar.collect.KakaoClient;
import com.siseradar.collect.KakaoClient.LatLng;
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
 * Forward-geocodes one building in the background and caches the result. A geocoded point is only
 * accepted (SUCCESS) if it reverse-resolves back to the same 시군구 — guards against same-name
 * complexes in another 구. In-flight dedup via {@link #claim}.
 */
@Component
public class MapGeocodeWorker {

  private static final Logger log = LoggerFactory.getLogger(MapGeocodeWorker.class);

  private final KakaoClient kakao;
  private final ComplexGeocodeRepository repo;
  private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

  public MapGeocodeWorker(KakaoClient kakao, ComplexGeocodeRepository repo) {
    this.kakao = kakao;
    this.repo = repo;
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
      LatLng ll = kakao.geocode(query);
      if (ll != null) {
        // accept only if the point is actually in the expected 시군구
        String resolved = kakao.lawdCdAt(ll.lng(), ll.lat());
        if (lawdCd.equals(resolved)) {
          status = GeocodeStatus.SUCCESS;
          lat = ll.lat();
          lng = ll.lng();
        }
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
}
