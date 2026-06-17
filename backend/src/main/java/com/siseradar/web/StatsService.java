package com.siseradar.web;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.BandStatRow;
import com.siseradar.repository.ComplexChangeRow;
import com.siseradar.repository.ComplexRankRow;
import com.siseradar.repository.MonthlyStatRow;
import com.siseradar.repository.RealEstateTransactionRepository;
import com.siseradar.repository.SameStoreChangeRow;
import com.siseradar.web.dto.ComplexChangeResponse;
import com.siseradar.web.dto.ComplexRankResponse;
import com.siseradar.web.dto.MonthlyStatsResponse;
import com.siseradar.web.dto.SameStoreChangeResponse;
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
      String lawdCd, PropertyType pt, TradeType tt, String from, String to, int bucketMonths) {
    int bucket = (bucketMonths == 3 || bucketMonths == 6 || bucketMonths == 12) ? bucketMonths : 1;
    List<MonthlyStatRow> rows = repository.monthlyStats(lawdCd, pt.name(), tt.name(), from, to, bucket);

    // area-band breakdown, grouped by bucket
    Map<String, List<BandStat>> bandsByYm =
        repository.monthlyStatsByBand(lawdCd, pt.name(), tt.name(), from, to, bucket).stream()
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
              row.getMonthsInBucket(),
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
        repository.monthlyStats(lawdCd, pt.name(), tt.name(), fromYm, toYm, 1).stream()
            .collect(Collectors.toMap(MonthlyStatRow::getYm, MonthlyStatRow::getAvgPricePerArea));
    Double naive = null;
    Double f = regionAvg.get(fromYm);
    Double t = regionAvg.get(toYm);
    if (f != null && t != null && f != 0) {
      naive = round1((t - f) / f * 100.0);
    }

    return new ComplexChangeResponse(fromYm, toYm, pcts.size(), avg, median, naive);
  }

  private static final int CHANGE_MONTHS = 12;

  /**
   * 동일단지(같은 건물) 변동률 — 최근 12개월 vs 직전 12개월 고정 윈도(전국 데이터의 최신월 기준).
   * 지도 버블·대시보드 카드·AI 요약이 공유하는 단일 계산. 두 윈도에 모두 거래된 단지가 없으면
   * (24개월 미충족 등) hasData=false(데이터 부족).
   */
  public SameStoreChangeResponse sameStoreChange12(String lawdCd, PropertyType pt, TradeType tt) {
    String anchor = repository.globalLatestYmd(pt, tt);
    if (anchor == null) {
      return new SameStoreChangeResponse(false, null, null, 0, null, null, null, null);
    }
    YearMonth a = YearMonth.parse(anchor, YM);
    String curFrom = a.minusMonths(CHANGE_MONTHS - 1L).format(YM);
    String curTo = anchor;
    String prevFrom = a.minusMonths(2L * CHANGE_MONTHS - 1).format(YM);
    String prevTo = a.minusMonths(CHANGE_MONTHS).format(YM);

    SameStoreChangeRow r =
        repository.sameStoreChange12(lawdCd, pt.name(), tt.name(), curFrom, curTo, prevFrom, prevTo);
    boolean hasData = r.getMatched() > 0 && r.getAvgPct() != null;
    return new SameStoreChangeResponse(
        hasData,
        hasData ? round1(r.getAvgPct()) : null,
        hasData && r.getMedianPct() != null ? round1(r.getMedianPct()) : null,
        r.getMatched(),
        curFrom,
        curTo,
        prevFrom,
        prevTo);
  }

  private static Double round1(double v) {
    return Math.round(v * 10.0) / 10.0;
  }
}
