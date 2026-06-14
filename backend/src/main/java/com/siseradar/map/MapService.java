package com.siseradar.map;

import com.siseradar.domain.ComplexGeocode;
import com.siseradar.domain.GeocodeStatus;
import com.siseradar.domain.PropertyType;
import com.siseradar.domain.RegionCentroid;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.ComplexGeocodeRepository;
import com.siseradar.repository.MapComplexStatRow;
import com.siseradar.repository.MapRegionStatRow;
import com.siseradar.repository.RealEstateTransactionRepository;
import com.siseradar.repository.RegionCentroidRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Assembles map data. Region bubbles (low zoom) aggregate ALL transactions per 시군구 and sit on
 * the region's own cached centroid. Complex markers (high zoom) join per-building 전용 단위면적가
 * stats with the geocode cache; missing buildings are queued for background geocoding (capped) and
 * appear on later calls.
 */
@Service
public class MapService {

  /** Max new buildings to geocode per request (Kakao quota guard). */
  private static final int GEOCODE_CAP = 25;
  /** Max region centroids to geocode per /regions request (one-time, tiny). */
  private static final int CENTROID_CAP = 10;
  /** Max regions a single bbox request will pull complexes for. */
  private static final int BBOX_REGION_CAP = 12;

  /** Only these types get individual markers (geocodable building names). */
  private static final Set<PropertyType> MARKER_TYPES =
      EnumSet.of(PropertyType.APT, PropertyType.OFFICETEL, PropertyType.ROW_HOUSE);

  private final RealEstateTransactionRepository trades;
  private final ComplexGeocodeRepository geocodes;
  private final RegionCentroidRepository centroids;
  private final MapGeocodeWorker worker;
  private final RegionCentroidWorker centroidWorker;

  public MapService(
      RealEstateTransactionRepository trades,
      ComplexGeocodeRepository geocodes,
      RegionCentroidRepository centroids,
      MapGeocodeWorker worker,
      RegionCentroidWorker centroidWorker) {
    this.trades = trades;
    this.geocodes = geocodes;
    this.centroids = centroids;
    this.worker = worker;
    this.centroidWorker = centroidWorker;
  }

  /** Markers for one region (legacy/explicit path). */
  public List<MapComplexResponse> complexes(
      String lawdCd, PropertyType pt, TradeType tt, String from, String to, String band) {
    if (!MARKER_TYPES.contains(pt)) {
      return List.of(); // 단독/토지/상업/산업/분양권은 지역집계(버블)에서만
    }
    List<MapComplexResponse> out = new ArrayList<>();
    collectComplexes(lawdCd, pt, tt, from, to, band, out, new int[] {0});
    return out;
  }

  /**
   * Markers for every region whose centroid falls inside the viewport bbox — the high-zoom,
   * viewport-driven path. Geocoding is lazy and shares one cap across all visible regions.
   */
  public List<MapComplexResponse> complexesInBounds(
      double swLat,
      double swLng,
      double neLat,
      double neLng,
      PropertyType pt,
      TradeType tt,
      String from,
      String to,
      String band) {
    if (!MARKER_TYPES.contains(pt)) {
      return List.of();
    }
    List<RegionCentroid> regions = centroids.findInBounds(swLat, neLat, swLng, neLng);
    List<MapComplexResponse> out = new ArrayList<>();
    int[] budget = {0};
    int count = 0;
    for (RegionCentroid r : regions) {
      if (count++ >= BBOX_REGION_CAP) {
        break;
      }
      collectComplexes(r.getLawdCd(), pt, tt, from, to, band, out, budget);
    }
    return out;
  }

  /**
   * Shared per-region marker assembly; {@code launched[0]} is the running geocode budget. Uses the
   * period as given (null = 전체 기간) — the SAME scope as the region bubbles, so a cluster's 거래
   * 건수 합 converges toward the region total as geocoding fills in.
   */
  private void collectComplexes(
      String lawdCd,
      PropertyType pt,
      TradeType tt,
      String from,
      String to,
      String band,
      List<MapComplexResponse> out,
      int[] launched) {
    String fromYm = (from == null || from.isBlank()) ? null : from;
    String toYm = (to == null || to.isBlank()) ? null : to;
    String bandFilter = (band == null || band.isBlank()) ? null : band;

    List<MapComplexStatRow> rows =
        trades.mapComplexStats(lawdCd, pt.name(), tt.name(), fromYm, toYm, bandFilter);

    Map<String, ComplexGeocode> cache =
        geocodes.findByLawdCdAndPropertyType(lawdCd, pt).stream()
            .collect(Collectors.toMap(ComplexGeocode::getBuildingName, g -> g, (a, b) -> a));

    for (MapComplexStatRow row : rows) {
      ComplexGeocode g = cache.get(row.getBuildingName());
      if (g != null) {
        if (g.getStatus() == GeocodeStatus.SUCCESS && g.getLat() != null && g.getLng() != null) {
          out.add(
              new MapComplexResponse(
                  row.getBuildingName(),
                  g.getLat(),
                  g.getLng(),
                  Math.round(row.getAvgPricePerArea()),
                  Math.round(row.getMedianPricePerArea()),
                  row.getCnt()));
        }
        // PENDING/FAILED → skip
        continue;
      }
      // not cached → queue background geocoding (capped, in-flight dedup)
      if (launched[0] < GEOCODE_CAP) {
        String key = MapGeocodeWorker.key(lawdCd, pt, row.getBuildingName());
        if (worker.claim(key)) {
          worker.geocode(lawdCd, pt, row.getBuildingName(), row.getUmdNm(), key);
          launched[0]++;
        }
      }
    }
  }

  /**
   * Low-zoom region bubbles. Stats are over ALL matching transactions (전체 거래, 지오코딩 무관),
   * so volume matches the dashboard. The bubble sits on the 시군구's own centroid (cached, geocoded
   * once); regions whose centroid isn't cached yet are queued (capped) and appear on a later call.
   */
  public List<MapRegionResponse> regions(
      PropertyType pt, TradeType tt, String from, String to, String band) {
    String bandFilter = (band == null || band.isBlank()) ? null : band;
    List<MapRegionStatRow> stats = trades.regionStats(pt.name(), tt.name(), from, to, bandFilter);

    List<String> lawdCds = stats.stream().map(MapRegionStatRow::getLawdCd).toList();
    Map<String, RegionCentroid> cache =
        centroids.findAllById(lawdCds).stream()
            .collect(Collectors.toMap(RegionCentroid::getLawdCd, c -> c, (a, b) -> a));

    List<MapRegionResponse> out = new ArrayList<>();
    int launched = 0;
    for (MapRegionStatRow s : stats) {
      RegionCentroid c = cache.get(s.getLawdCd());
      if (c != null) {
        if (c.getStatus() == GeocodeStatus.SUCCESS && c.getLat() != null && c.getLng() != null) {
          out.add(
              new MapRegionResponse(
                  s.getLawdCd(),
                  c.getLat(),
                  c.getLng(),
                  Math.round(s.getAvgPricePerArea()),
                  Math.round(s.getMedianPricePerArea()),
                  s.getCnt()));
        }
        // FAILED → no bubble (don't retry)
        continue;
      }
      // centroid not cached → queue one-time geocode (capped, in-flight dedup)
      if (launched < CENTROID_CAP && centroidWorker.claim(s.getLawdCd())) {
        centroidWorker.geocode(s.getLawdCd());
        launched++;
      }
    }
    return out;
  }
}
