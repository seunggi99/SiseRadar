package com.siseradar.web;

import com.siseradar.repository.AptTradeRepository;
import com.siseradar.repository.ComplexRankRow;
import com.siseradar.repository.MonthlyStatRow;
import com.siseradar.web.dto.ComplexRankResponse;
import com.siseradar.web.dto.MonthlyStatsResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

  private final AptTradeRepository repository;

  public StatsService(AptTradeRepository repository) {
    this.repository = repository;
  }

  /**
   * Monthly aggregates (count/avg/median/price-per-pyeong via GROUP BY), with month-over-month
   * average-price change computed across the ordered result.
   */
  public List<MonthlyStatsResponse> monthly(String lawdCd, String from, String to) {
    List<MonthlyStatRow> rows = repository.monthlyStats(lawdCd, from, to);
    List<MonthlyStatsResponse> out = new ArrayList<>(rows.size());
    Double prevAvg = null;
    for (MonthlyStatRow row : rows) {
      double avg = row.getAvgAmount();
      Double mom =
          prevAvg != null && prevAvg != 0
              ? Math.round((avg - prevAvg) / prevAvg * 1000.0) / 10.0
              : null;
      out.add(
          new MonthlyStatsResponse(
              row.getYm(),
              row.getCnt(),
              Math.round(avg),
              Math.round(row.getMedianAmount()),
              Math.round(row.getAvgPricePerPyeong()),
              mom));
      prevAvg = avg;
    }
    return out;
  }

  /**
   * Per-complex ranking for a region+month, ranked by average price. When {@code ym} is omitted,
   * the most recent month with data for that region is used.
   */
  public List<ComplexRankResponse> complexes(String lawdCd, String ym) {
    String month = (ym == null || ym.isBlank()) ? repository.latestYmd(lawdCd) : ym;
    if (month == null) {
      return List.of();
    }
    List<ComplexRankRow> rows = repository.complexRanking(lawdCd, month);
    List<ComplexRankResponse> out = new ArrayList<>(rows.size());
    int rank = 1;
    for (ComplexRankRow row : rows) {
      out.add(
          new ComplexRankResponse(
              rank++,
              row.getAptName(),
              row.getCnt(),
              Math.round(row.getAvgAmount()),
              row.getMaxAmount(),
              Math.round(row.getAvgPricePerPyeong())));
    }
    return out;
  }
}
