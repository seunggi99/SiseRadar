package com.siseradar.web;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.BandStatRow;
import com.siseradar.repository.ComplexRankRow;
import com.siseradar.repository.MonthlyStatRow;
import com.siseradar.repository.RealEstateTransactionRepository;
import com.siseradar.web.dto.ComplexRankResponse;
import com.siseradar.web.dto.MonthlyStatsResponse;
import com.siseradar.web.dto.MonthlyStatsResponse.BandStat;
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
}
