package com.siseradar.map;

import com.siseradar.domain.ComplexGeocode;
import com.siseradar.domain.GeocodeStatus;
import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.ComplexGeocodeRepository;
import com.siseradar.repository.MapComplexStatRow;
import com.siseradar.repository.RealEstateTransactionRepository;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Assembles map markers: per-building 전용 단위면적가 stats joined with the geocode cache. Buildings
 * without coords are queued for background geocoding (capped per request) and appear on later calls.
 */
@Service
public class MapService {

  private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");
  /** Default window so the map isn't empty. */
  private static final int DEFAULT_MONTHS = 12;
  /** Max new buildings to geocode per request (Kakao quota guard). */
  private static final int GEOCODE_CAP = 25;

  /** Only these types get individual markers (geocodable building names). */
  private static final Set<PropertyType> MARKER_TYPES =
      EnumSet.of(PropertyType.APT, PropertyType.OFFICETEL, PropertyType.ROW_HOUSE);

  private final RealEstateTransactionRepository trades;
  private final ComplexGeocodeRepository geocodes;
  private final MapGeocodeWorker worker;

  public MapService(
      RealEstateTransactionRepository trades,
      ComplexGeocodeRepository geocodes,
      MapGeocodeWorker worker) {
    this.trades = trades;
    this.geocodes = geocodes;
    this.worker = worker;
  }

  public List<MapComplexResponse> complexes(
      String lawdCd, PropertyType pt, TradeType tt, String from, String to, String band) {
    if (!MARKER_TYPES.contains(pt)) {
      return List.of(); // 단독/토지/상업/산업/분양권은 지역집계(증분3)에서만
    }
    String toYm = (to == null || to.isBlank()) ? trades.latestYmd(lawdCd, pt, tt) : to;
    if (toYm == null) {
      return List.of();
    }
    String fromYm =
        (from == null || from.isBlank())
            ? YearMonth.parse(toYm, YM).minusMonths(DEFAULT_MONTHS).format(YM)
            : from;
    String bandFilter = (band == null || band.isBlank()) ? null : band;

    List<MapComplexStatRow> rows =
        trades.mapComplexStats(lawdCd, pt.name(), tt.name(), fromYm, toYm, bandFilter);

    Map<String, ComplexGeocode> cache =
        geocodes.findByLawdCdAndPropertyType(lawdCd, pt).stream()
            .collect(Collectors.toMap(ComplexGeocode::getBuildingName, g -> g, (a, b) -> a));

    List<MapComplexResponse> out = new ArrayList<>();
    int launched = 0;
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
      if (launched < GEOCODE_CAP) {
        String key = MapGeocodeWorker.key(lawdCd, pt, row.getBuildingName());
        if (worker.claim(key)) {
          worker.geocode(lawdCd, pt, row.getBuildingName(), row.getUmdNm(), key);
          launched++;
        }
      }
    }
    return out;
  }
}
