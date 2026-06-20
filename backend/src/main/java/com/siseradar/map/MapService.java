package com.siseradar.map;

import com.siseradar.domain.ComplexGeocode;
import com.siseradar.domain.GeocodeStatus;
import com.siseradar.domain.PropertyType;
import com.siseradar.domain.RegionCentroid;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.BuildingRow;
import com.siseradar.repository.ComplexGeocodeRepository;
import com.siseradar.repository.MapComplexStatRow;
import com.siseradar.repository.MapRegionStatRow;
import com.siseradar.repository.RealEstateTransactionRepository;
import com.siseradar.repository.ComplexPeriodRow;
import com.siseradar.repository.MapMarkerRow;
import com.siseradar.repository.RegionCentroidRepository;
import com.siseradar.repository.RegionChangeRow;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");
  /** Length of the current/prior comparison windows for 단지 변동률. */
  private static final int CHANGE_MONTHS = 12;
  /** Max new buildings to geocode per request (Kakao quota guard). */
  private static final int GEOCODE_CAP = 25;
  /** Max region centroids to geocode per /regions request (one-time, tiny). */
  private static final int CENTROID_CAP = 10;
  /** Max regions a single bbox request will pull complexes for. */
  private static final int BBOX_REGION_CAP = 12;

  /** Only these types get individual markers (geocodable building names). */
  private static final Set<PropertyType> MARKER_TYPES =
      EnumSet.of(PropertyType.APT, PropertyType.OFFICETEL, PropertyType.ROW_HOUSE);

  /** Region-bubble aggregates change only on new collection → short-TTL cache (5.7s query → instant). */
  private static final long REGIONS_TTL_MS = 10 * 60 * 1000L;

  private record CachedRegions(long at, List<MapRegionResponse> data) {}

  private final Map<String, CachedRegions> regionsCache = new ConcurrentHashMap<>();

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
   * Eagerly geocode ALL mappable buildings of a region (synchronous, sequential) so markers render
   * in one shot instead of trickling in via the lazy 25/request cap. Idempotent — already-cached
   * buildings are skipped, so re-runs resume. A transient Kakao error propagates (warm job backs
   * off) leaving the rest uncached. Returns {attempted, success, skipped}.
   */
  public Map<String, Integer> warmRegion(String lawdCd) {
    int attempted = 0;
    int success = 0;
    int skipped = 0;
    for (PropertyType pt : MARKER_TYPES) {
      Set<String> cached =
          geocodes.findByLawdCdAndPropertyType(lawdCd, pt).stream()
              .map(ComplexGeocode::getBuildingName)
              .collect(Collectors.toSet());
      for (BuildingRow b : trades.distinctBuildings(lawdCd, pt.name())) {
        if (cached.contains(b.getBuildingName())) {
          skipped++;
          continue;
        }
        attempted++;
        if (worker.geocodeSync(lawdCd, pt, b.getBuildingName(), b.getUmdNm())) {
          success++;
        }
      }
    }
    return Map.of("attempted", attempted, "success", success, "skipped", skipped);
  }

  /**
   * Markers for the viewport bbox — the high-zoom, viewport-driven path. Markers are selected by
   * each complex's OWN coordinate (so they render at any zoom, incl. max zoom-in), while lazy
   * geocoding of not-yet-cached complexes is triggered for regions overlapping the box.
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
    String fromYm = (from == null || from.isBlank()) ? null : from;
    String toYm = (to == null || to.isBlank()) ? null : to;
    String bandFilter = (band == null || band.isBlank()) ? null : band;
    Windows w = changeWindows(pt, tt);

    // markers: geocoded complexes whose coordinates are inside the viewport (any zoom)
    List<MapComplexResponse> out = new ArrayList<>();
    for (MapMarkerRow r :
        trades.markersInBbox(
            swLat, neLat, swLng, neLng, pt.name(), tt.name(), fromYm, toYm, bandFilter,
            w.curFrom(), w.curTo(), w.prevFrom(), w.prevTo())) {
      out.add(
          new MapComplexResponse(
              r.getLawdCd(),
              r.getBuildingName(),
              r.getLat(),
              r.getLng(),
              Math.round(r.getAvgPricePerArea()),
              Math.round(r.getMedianPricePerArea()),
              r.getCnt(),
              markerChangePct(r)));
    }

    // discovery: queue lazy geocoding for not-yet-cached complexes of overlapping regions
    int[] budget = {0};
    int count = 0;
    for (RegionCentroid rc : centroids.findInBounds(swLat, neLat, swLng, neLng)) {
      if (count++ >= BBOX_REGION_CAP || budget[0] >= GEOCODE_CAP) {
        break;
      }
      triggerGeocoding(rc.getLawdCd(), pt, tt, fromYm, toYm, bandFilter, budget);
    }
    return out;
  }

  /** Queue background geocoding for a region's not-yet-cached buildings (capped, in-flight dedup). */
  private void triggerGeocoding(
      String lawdCd, PropertyType pt, TradeType tt, String from, String to, String band, int[] launched) {
    Map<String, ComplexGeocode> cache =
        geocodes.findByLawdCdAndPropertyType(lawdCd, pt).stream()
            .collect(Collectors.toMap(ComplexGeocode::getBuildingName, g -> g, (a, b) -> a));
    for (MapComplexStatRow row : trades.mapComplexStats(lawdCd, pt.name(), tt.name(), from, to, band)) {
      if (launched[0] >= GEOCODE_CAP) {
        break;
      }
      if (cache.containsKey(row.getBuildingName())) {
        continue; // already SUCCESS/PENDING/FAILED — don't re-queue
      }
      String key = MapGeocodeWorker.key(lawdCd, pt, row.getBuildingName());
      if (worker.claim(key)) {
        worker.geocode(lawdCd, pt, row.getBuildingName(), row.getUmdNm(), key);
        launched[0]++;
      }
    }
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
                  lawdCd,
                  row.getBuildingName(),
                  g.getLat(),
                  g.getLng(),
                  Math.round(row.getAvgPricePerArea()),
                  Math.round(row.getMedianPricePerArea()),
                  row.getCnt(),
                  null)); // legacy per-region path doesn't compute 변동률
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
    String cacheKey = pt + "|" + tt + "|" + from + "|" + to + "|" + band;
    CachedRegions hit = regionsCache.get(cacheKey);
    long now = System.currentTimeMillis();
    if (hit != null && now - hit.at() < REGIONS_TTL_MS) {
      return hit.data();
    }
    List<MapRegionResponse> out = computeRegions(pt, tt, from, to, band);
    regionsCache.put(cacheKey, new CachedRegions(now, out));
    return out;
  }

  private List<MapRegionResponse> computeRegions(
      PropertyType pt, TradeType tt, String from, String to, String band) {
    String bandFilter = (band == null || band.isBlank()) ? null : band;
    List<MapRegionStatRow> stats = trades.regionStats(pt.name(), tt.name(), from, to, bandFilter);

    List<String> lawdCds = stats.stream().map(MapRegionStatRow::getLawdCd).toList();
    Map<String, RegionCentroid> cache =
        centroids.findAllById(lawdCds).stream()
            .collect(Collectors.toMap(RegionCentroid::getLawdCd, c -> c, (a, b) -> a));

    // 동일단지(same-store) 1년 변동률 per region — for the 상승률 색 모드 (data 부족 region → absent)
    Windows w = changeWindows(pt, tt);
    Map<String, Double> changeByRegion = new HashMap<>();
    for (RegionChangeRow rc :
        trades.regionChange(
            pt.name(), tt.name(), bandFilter, w.curFrom(), w.curTo(), w.prevFrom(), w.prevTo())) {
      if (rc.getChangePct() != null) {
        changeByRegion.put(rc.getLawdCd(), Math.round(rc.getChangePct() * 10) / 10.0);
      }
    }

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
                  s.getCnt(),
                  changeByRegion.get(s.getLawdCd())));
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

  /**
   * One building's 평당가(전용) 변동률: current 12 months vs the preceding 12 months, on the map's
   * global anchor (so it matches the marker's color), optionally within one 평형대. Both windows
   * must have transactions — otherwise {@code hasData=false} ("변동 데이터 부족"), never a misleading 0%.
   */
  public MapComplexChangeResponse complexChange(
      String lawdCd, PropertyType pt, TradeType tt, String buildingName, String band) {
    Windows w = changeWindows(pt, tt);
    if (w.curTo() == null) {
      return new MapComplexChangeResponse(false, null, 0, 0, null, null, null, null);
    }
    String bandFilter = (band == null || band.isBlank()) ? null : band;

    ComplexPeriodRow cur =
        trades.complexPeriodStat(
            lawdCd, pt.name(), tt.name(), buildingName, w.curFrom(), w.curTo(), bandFilter);
    ComplexPeriodRow prev =
        trades.complexPeriodStat(
            lawdCd, pt.name(), tt.name(), buildingName, w.prevFrom(), w.prevTo(), bandFilter);

    boolean hasData =
        cur.getCnt() > 0
            && prev.getCnt() > 0
            && cur.getAvgPricePerArea() != null
            && prev.getAvgPricePerArea() != null
            && prev.getAvgPricePerArea() != 0;
    if (!hasData) {
      return new MapComplexChangeResponse(
          false, null, cur.getCnt(), prev.getCnt(), w.curFrom(), w.curTo(), w.prevFrom(), w.prevTo());
    }
    double pct =
        (cur.getAvgPricePerArea() - prev.getAvgPricePerArea()) / prev.getAvgPricePerArea() * 100.0;
    return new MapComplexChangeResponse(
        true,
        Math.round(pct * 10) / 10.0,
        cur.getCnt(),
        prev.getCnt(),
        w.curFrom(),
        w.curTo(),
        w.prevFrom(),
        w.prevTo());
  }

  /** Current / prior 12-month windows for the 변동률, on the dataset's latest month for this type. */
  private record Windows(String curFrom, String curTo, String prevFrom, String prevTo) {}

  private Windows changeWindows(PropertyType pt, TradeType tt) {
    String anchor = trades.globalLatestYmd(pt, tt);
    if (anchor == null) {
      return new Windows(null, null, null, null); // no data → BETWEEN never matches
    }
    YearMonth a = YearMonth.parse(anchor, YM);
    return new Windows(
        a.minusMonths(CHANGE_MONTHS - 1L).format(YM),
        anchor,
        a.minusMonths(2L * CHANGE_MONTHS - 1).format(YM),
        a.minusMonths(CHANGE_MONTHS).format(YM));
  }

  /** 마커의 1년 변동률(%) — 두 12개월 윈도 모두 거래가 있을 때만, 아니면 null(데이터 부족). */
  private static Double markerChangePct(MapMarkerRow r) {
    if (r.getCurCnt() > 0
        && r.getPrevCnt() > 0
        && r.getCurAvg() != null
        && r.getPrevAvg() != null
        && r.getPrevAvg() != 0) {
      return Math.round((r.getCurAvg() - r.getPrevAvg()) / r.getPrevAvg() * 1000) / 10.0;
    }
    return null;
  }
}
