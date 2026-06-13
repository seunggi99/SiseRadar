package com.siseradar.repository;

import com.siseradar.domain.AptTrade;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AptTradeRepository extends JpaRepository<AptTrade, Long> {

  /** Existing rows for one region+month — used to build the idempotency key set. */
  List<AptTrade> findByLawdCdAndDealYmd(String lawdCd, String dealYmd);

  /** Filtered, paginated transaction search. Optional params are treated as "no filter" when null. */
  @Query(
      """
      SELECT t FROM AptTrade t
      WHERE t.lawdCd = :lawdCd
        AND (:aptName IS NULL OR t.aptName = :aptName)
        AND (:from IS NULL OR t.dealYmd >= :from)
        AND (:to IS NULL OR t.dealYmd <= :to)
        AND (:areaMin IS NULL OR t.area >= :areaMin)
        AND (:areaMax IS NULL OR t.area <= :areaMax)
      ORDER BY t.dealDate DESC, t.id DESC
      """)
  Page<AptTrade> search(
      @Param("lawdCd") String lawdCd,
      @Param("aptName") String aptName,
      @Param("from") String from,
      @Param("to") String to,
      @Param("areaMin") BigDecimal areaMin,
      @Param("areaMax") BigDecimal areaMax,
      Pageable pageable);

  /**
   * Monthly aggregates computed server-side via GROUP BY. {@code median} uses the
   * ordered-set aggregate {@code PERCENTILE_CONT}, supported by both H2 and PostgreSQL.
   * Amounts are in 만원; price-per-pyeong is 만원/평 (평 = ㎡ / 3.305785).
   */
  @Query(
      value =
          """
          SELECT t.deal_ymd AS ym,
                 COUNT(*) AS cnt,
                 AVG(t.deal_amount) AS avgAmount,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY t.deal_amount) AS medianAmount,
                 AVG(t.deal_amount / (t.area / 3.305785)) AS avgPricePerPyeong
          FROM apt_trade t
          WHERE t.lawd_cd = :lawdCd
            AND (:from IS NULL OR t.deal_ymd >= :from)
            AND (:to IS NULL OR t.deal_ymd <= :to)
          GROUP BY t.deal_ymd
          ORDER BY t.deal_ymd
          """,
      nativeQuery = true)
  List<MonthlyStatRow> monthlyStats(
      @Param("lawdCd") String lawdCd, @Param("from") String from, @Param("to") String to);

  /** Per-complex aggregates for one region+month, ranked by average price (만원). */
  @Query(
      value =
          """
          SELECT t.apt_name AS aptName,
                 COUNT(*) AS cnt,
                 AVG(t.deal_amount) AS avgAmount,
                 MAX(t.deal_amount) AS maxAmount,
                 AVG(t.deal_amount / (t.area / 3.305785)) AS avgPricePerPyeong
          FROM apt_trade t
          WHERE t.lawd_cd = :lawdCd AND t.deal_ymd = :ym
          GROUP BY t.apt_name
          ORDER BY avgAmount DESC
          """,
      nativeQuery = true)
  List<ComplexRankRow> complexRanking(@Param("lawdCd") String lawdCd, @Param("ym") String ym);

  /** Most recent deal_ymd present for a region (used to default the dashboard month). */
  @Query("SELECT MAX(t.dealYmd) FROM AptTrade t WHERE t.lawdCd = :lawdCd")
  String latestYmd(@Param("lawdCd") String lawdCd);

  /** Distinct months collected for a region — drives on-demand collection state. */
  @Query("SELECT COUNT(DISTINCT t.dealYmd) FROM AptTrade t WHERE t.lawdCd = :lawdCd")
  long countMonths(@Param("lawdCd") String lawdCd);

  /** Average price (만원) for a region+month, or null if no rows — for alert evaluation. */
  @Query("SELECT AVG(t.dealAmount) FROM AptTrade t WHERE t.lawdCd = :lawdCd AND t.dealYmd = :ym")
  Double avgAmountByRegionAndYm(@Param("lawdCd") String lawdCd, @Param("ym") String ym);

  /** Average price (만원) for a single complex in a region+month, or null if none. */
  @Query(
      "SELECT AVG(t.dealAmount) FROM AptTrade t "
          + "WHERE t.lawdCd = :lawdCd AND t.aptName = :aptName AND t.dealYmd = :ym")
  Double avgAmountByComplexAndYm(
      @Param("lawdCd") String lawdCd, @Param("aptName") String aptName, @Param("ym") String ym);
}
