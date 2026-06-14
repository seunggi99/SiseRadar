package com.siseradar.web;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.BandStatRow;
import com.siseradar.repository.ComplexChangeRow;
import com.siseradar.repository.ComplexRankRow;
import com.siseradar.repository.MonthlyStatRow;
import com.siseradar.repository.RealEstateTransactionRepository;
import com.siseradar.web.dto.ComplexChangeResponse;
import com.siseradar.web.dto.ComplexRankResponse;
import com.siseradar.web.dto.MonthlyStatsResponse;
import com.siseradar.web.dto.MonthlyStatsResponse.BandStat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Aggregations are scoped by (propertyType, tradeType). "Amount" is the primary amount
 * (매매 거래가 / 전월세 보증금); {@code avgMonthlyRent} is non-null only for RENT.
 */
@Service
public class StatsService {

  private final RealEstateTransactionRepository repository;

  public StatsService(RealEstateTransactionRepository repository) {
    this.repository = repository;
  }

  public List<MonthlyStatsResponse> monthly(
      String lawdCd, PropertyType pt, TradeType tt, String from, String to) {
    List<MonthlyStatRow> rows = repository.monthlyStats(lawdCd, pt.name(), tt.name(), from, to);

    // area-band breakdown, grouped by month
    Map<String, List<BandStat>> bandsByYm =
        repository.monthlyStatsByBand(lawdCd, pt.name(), tt.name(), from, to).stream()
            .collect(
                Collectors.groupingBy(
                    BandStatRow::getYm,
                    Collectors.mapping(
                        b ->
                            new BandStat(
                                b.getBand(),
                                b.getCnt(),
                                Math.round(b.getAvgPricePerArea()),
                                Math.round(b.getMedianPricePerArea())),
                        Collectors.toList())));

    List<MonthlyStatsResponse> out = new ArrayList<>(rows.size());
    // MoM is based on the median 단위면적가 (less composition-biased than avg 거래가).
    Double prevMedianPerArea = null;
    for (MonthlyStatRow row : rows) {
      double medPerArea = row.getMedianPricePerArea();
      Double mom =
          prevMedianPerArea != null && prevMedianPerArea != 0
              ? Math.round((medPerArea - prevMedianPerArea) / prevMedianPerArea * 1000.0) / 10.0
              : null;
      out.add(
          new MonthlyStatsResponse(
              row.getYm(),
              row.getCnt(),
              Math.round(row.getAvgAmount()),
              Math.round(row.getMedianAmount()),
              Math.round(row.getAvgPricePerArea()),
              Math.round(row.getMedianPricePerArea()),
              row.getAvgMonthlyRent() == null ? null : Math.round(row.getAvgMonthlyRent()),
              mom,
              bandsByYm.getOrDefault(row.getYm(), List.of())));
      prevMedianPerArea = medPerArea;
    }
    return out;
  }

  /** When {@code ym} is omitted, the most recent month with data for that region+type is used. */
  public List<ComplexRankResponse> complexes(
      String lawdCd, PropertyType pt, TradeType tt, String ym) {
    String month = (ym == null || ym.isBlank()) ? repository.latestYmd(lawdCd, pt, tt) : ym;
    if (month == null) {
      return List.of();
    }
    List<ComplexRankRow> rows = repository.complexRanking(lawdCd, pt.name(), tt.name(), month);
    List<ComplexRankResponse> out = new ArrayList<>(rows.size());
    int rank = 1;
    for (ComplexRankRow row : rows) {
      out.add(
          new ComplexRankResponse(
              rank++,
              row.getBuildingName(),
              row.getCnt(),
              Math.round(row.getAvgAmount()),
              row.getMaxAmount(),
              Math.round(row.getAvgPricePerPyeong()),
              row.getAvgMonthlyRent() == null ? null : Math.round(row.getAvgMonthlyRent())));
    }
    return out;
  }

  private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

  /**
   * 동일 단지 변동률: 두 시점에 모두 거래된 같은 건물+평형대만 골라 단위면적가 % 변동의
   * 평균·중위를 계산(구성 편향 통제). to 미지정 시 최신월, from 미지정 시 그 12개월 전.
   */
  public ComplexChangeResponse complexChange(
      String lawdCd, PropertyType pt, TradeType tt, String from, String to) {
    String toYm = (to == null || to.isBlank()) ? repository.latestYmd(lawdCd, pt, tt) : to;
    if (toYm == null) {
      return new ComplexChangeResponse(null, null, 0, null, null, null);
    }
    String fromYm =
        (from == null || from.isBlank())
            ? YearMonth.parse(toYm, YM).minusMonths(12).format(YM)
            : from;

    List<ComplexChangeRow> rows = repository.complexChange(lawdCd, pt.name(), tt.name(), fromYm, toYm);
    List<Double> pcts =
        rows.stream()
            .filter(r -> r.getFromAvg() != 0)
            .map(r -> (r.getToAvg() - r.getFromAvg()) / r.getFromAvg() * 100.0)
            .sorted()
            .toList();

    Double avg = pcts.isEmpty() ? null : round1(pcts.stream().mapToDouble(d -> d).average().orElse(0));
    Double median = pcts.isEmpty() ? null : round1(pcts.get(pcts.size() / 2));

    // naive contrast: region-wide avg 단위면적가 change between the same two months
    Map<String, Double> regionAvg =
        repository.monthlyStats(lawdCd, pt.name(), tt.name(), fromYm, toYm).stream()
            .collect(Collectors.toMap(MonthlyStatRow::getYm, MonthlyStatRow::getAvgPricePerArea));
    Double naive = null;
    Double f = regionAvg.get(fromYm);
    Double t = regionAvg.get(toYm);
    if (f != null && t != null && f != 0) {
      naive = round1((t - f) / f * 100.0);
    }

    return new ComplexChangeResponse(fromYm, toYm, pcts.size(), avg, median, naive);
  }

  private static Double round1(double v) {
    return Math.round(v * 10.0) / 10.0;
  }
}
