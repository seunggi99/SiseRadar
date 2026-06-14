package com.siseradar.map;

import com.siseradar.collect.KakaoClient;
import com.siseradar.collect.KakaoClient.LatLng;
import com.siseradar.collect.KoreaRegions;
import com.siseradar.domain.GeocodeStatus;
import com.siseradar.domain.RegionCentroid;
import com.siseradar.repository.RegionCentroidRepository;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Geocodes a 시군구's own centroid once (address search on the region name) and persists it. Only
 * runs for regions that actually have transaction data, so the cost is a handful of one-time calls
 * (250 max ever) — no per-request Kakao usage for the map's region bubbles. In-flight dedup via
 * {@link #claim}.
 */
@Component
public class RegionCentroidWorker {

  private static final Logger log = LoggerFactory.getLogger(RegionCentroidWorker.class);

  private final KakaoClient kakao;
  private final KoreaRegions koreaRegions;
  private final RegionCentroidRepository repo;
  private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

  public RegionCentroidWorker(
      KakaoClient kakao, KoreaRegions koreaRegions, RegionCentroidRepository repo) {
    this.kakao = kakao;
    this.koreaRegions = koreaRegions;
    this.repo = repo;
  }

  /** Atomically reserve a region for centroid geocoding; false if already in flight. */
  public boolean claim(String lawdCd) {
    return inFlight.add(lawdCd);
  }

  @Async
  @Transactional
  public void geocode(String lawdCd) {
    try {
      GeocodeStatus status = GeocodeStatus.FAILED;
      Double lat = null;
      Double lng = null;

      String name = koreaRegions.name(lawdCd);
      if (name != null) {
        LatLng ll = kakao.geocodeAddress(name);
        if (ll != null) {
          status = GeocodeStatus.SUCCESS;
          lat = ll.lat();
          lng = ll.lng();
        }
      }
      repo.save(new RegionCentroid(lawdCd, lat, lng, status));
    } catch (DataIntegrityViolationException dup) {
      // already cached by another worker — fine
    } catch (RuntimeException e) {
      log.warn("Region centroid geocode failed {}: {}", lawdCd, e.getMessage());
      try {
        repo.save(new RegionCentroid(lawdCd, null, null, GeocodeStatus.FAILED));
      } catch (RuntimeException ignored) {
        /* row may already exist */
      }
    } finally {
      inFlight.remove(lawdCd);
    }
  }
}
