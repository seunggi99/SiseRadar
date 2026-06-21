package com.siseradar.map;

import com.siseradar.domain.ComplexGeocode;
import com.siseradar.domain.PropertyType;
import com.siseradar.domain.RegionCentroid;
import com.siseradar.repository.BuildingRow;
import com.siseradar.repository.ComplexGeocodeRepository;
import com.siseradar.repository.RealEstateTransactionRepository;
import com.siseradar.repository.RegionCentroidRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Background discovery for the viewport: queues lazy geocoding of not-yet-cached buildings in the
 * current bbox. Runs {@link Async} so it stays OFF the /api/map/complexes response critical path —
 * the markers come straight from {@code markersInBbox}, while uncached buildings are queued here.
 * Uses the cheap {@code distinctBuildings} (단지명 목록만) instead of the heavy per-building stats
 * aggregation — we only need building names to geocode, not medians.
 */
@Component
public class MapDiscovery {

  /** Max new buildings to queue per request (Kakao quota guard). */
  private static final int GEOCODE_CAP = 25;
  /** Max regions a single bbox request will scan for uncached buildings. */
  private static final int BBOX_REGION_CAP = 12;

  private final RealEstateTransactionRepository trades;
  private final ComplexGeocodeRepository geocodes;
  private final RegionCentroidRepository centroids;
  private final MapGeocodeWorker worker;

  public MapDiscovery(
      RealEstateTransactionRepository trades,
      ComplexGeocodeRepository geocodes,
      RegionCentroidRepository centroids,
      MapGeocodeWorker worker) {
    this.trades = trades;
    this.geocodes = geocodes;
    this.centroids = centroids;
    this.worker = worker;
  }

  @Async
  public void discover(double swLat, double swLng, double neLat, double neLng, PropertyType pt) {
    int[] budget = {0};
    int count = 0;
    for (RegionCentroid rc : centroids.findInBounds(swLat, neLat, swLng, neLng)) {
      if (count++ >= BBOX_REGION_CAP || budget[0] >= GEOCODE_CAP) {
        break;
      }
      queueRegion(rc.getLawdCd(), pt, budget);
    }
  }

  /** Queue geocoding for a region's not-yet-cached buildings (capped, in-flight dedup). */
  private void queueRegion(String lawdCd, PropertyType pt, int[] launched) {
    Set<String> cached =
        geocodes.findByLawdCdAndPropertyType(lawdCd, pt).stream()
            .map(ComplexGeocode::getBuildingName)
            .collect(Collectors.toSet());
    for (BuildingRow b : trades.distinctBuildings(lawdCd, pt.name())) {
      if (launched[0] >= GEOCODE_CAP) {
        break;
      }
      if (cached.contains(b.getBuildingName())) {
        continue; // 이미 SUCCESS/FAILED/PENDING — 재큐잉 안 함
      }
      String key = MapGeocodeWorker.key(lawdCd, pt, b.getBuildingName());
      if (worker.claim(key)) {
        worker.geocode(lawdCd, pt, b.getBuildingName(), b.getUmdNm(), key);
        launched[0]++;
      }
    }
  }
}
