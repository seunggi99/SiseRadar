package com.siseradar.repository;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.RealEstateTransaction;
import com.siseradar.domain.TradeType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregations are scoped by (propertyType, tradeType) so SALE 거래가 and RENT 보증금 never mix.
 * The "primary amount" is {@code COALESCE(deal_amount, deposit)} — deal_amount for SALE,
 * deposit for RENT. Native queries take the enum {@code name()} as a String.
 */
public interface RealEstateTransactionRepository extends JpaRepository<RealEstateTransaction, Long> {

  /** Existing rows for one region+month+type — used to build the idempotency key set. */
  List<RealEstateTransaction> findByLawdCdAndDealYmdAndPropertyTypeAndTradeType(
      String lawdCd, String dealYmd, PropertyType propertyType, TradeType tradeType);

  @Query(
      """
      SELECT t FROM RealEstateTransaction t
      WHERE t.lawdCd = :lawdCd
        AND t.propertyType = :propertyType
        AND t.tradeType = :tradeType
        AND (:buildingName IS NULL OR t.buildingName = :buildingName)
        AND (:from IS NULL OR t.dealYmd >= :from)
        AND (:to IS NULL OR t.dealYmd <= :to)
        AND (:areaMin IS NULL OR t.area >= :areaMin)
        AND (:areaMax IS NULL OR t.area <= :areaMax)
      ORDER BY t.dealDate DESC, t.id DESC
      """)
  Page<RealEstateTransaction> search(
      @Param("lawdCd") String lawdCd,
      @Param("propertyType") PropertyType propertyType,
      @Param("tradeType") TradeType tradeType,
      @Param("buildingName") String buildingName,
      @Param("from") String from,
      @Param("to") String to,
      @Param("areaMin") BigDecimal areaMin,
      @Param("areaMax") BigDecimal areaMax,
      Pageable pageable);

  @Query(
      value =
          """
          SELECT t.deal_ymd AS ym,
                 COUNT(*) AS cnt,
                 AVG(COALESCE(t.deal_amount, t.deposit)) AS avgAmount,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY COALESCE(t.deal_amount, t.deposit)) AS medianAmount,
                 AVG(COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS avgPricePerArea,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS medianPricePerArea,
                 AVG(t.monthly_rent) AS avgMonthlyRent
          FROM real_estate_transaction t
          WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt
            AND (:from IS NULL OR t.deal_ymd >= :from)
            AND (:to IS NULL OR t.deal_ymd <= :to)
          GROUP BY t.deal_ymd
          ORDER BY t.deal_ymd
          """,
      nativeQuery = true)
  List<MonthlyStatRow> monthlyStats(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("from") String from,
      @Param("to") String to);

  /**
   * Per-month, per-area-band breakdown (전용면적 기준 구간). Bands: ≤60 / 60–85 / 85–135 / >135.
   * Rows with null area land in LARGE (rare; residential types always have area).
   */
  @Query(
      value =
          """
          SELECT t.deal_ymd AS ym,
                 CASE WHEN t.area <= 60 THEN 'SMALL'
                      WHEN t.area <= 85 THEN 'MID_SMALL'
                      WHEN t.area <= 135 THEN 'MID_LARGE'
                      ELSE 'LARGE' END AS band,
                 COUNT(*) AS cnt,
                 AVG(COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS avgPricePerArea,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS medianPricePerArea
          FROM real_estate_transaction t
          WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt
            AND (:from IS NULL OR t.deal_ymd >= :from)
            AND (:to IS NULL OR t.deal_ymd <= :to)
          GROUP BY t.deal_ymd,
                 CASE WHEN t.area <= 60 THEN 'SMALL'
                      WHEN t.area <= 85 THEN 'MID_SMALL'
                      WHEN t.area <= 135 THEN 'MID_LARGE'
                      ELSE 'LARGE' END
          ORDER BY t.deal_ymd
          """,
      nativeQuery = true)
  List<BandStatRow> monthlyStatsByBand(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("from") String from,
      @Param("to") String to);

  @Query(
      value =
          """
          SELECT t.building_name AS buildingName,
                 COUNT(*) AS cnt,
                 AVG(COALESCE(t.deal_amount, t.deposit)) AS avgAmount,
                 MAX(COALESCE(t.deal_amount, t.deposit)) AS maxAmount,
                 AVG(COALESCE(t.deal_amount, t.deposit) / (t.area / 3.3058)) AS avgPricePerPyeong,
                 AVG(t.monthly_rent) AS avgMonthlyRent
          FROM real_estate_transaction t
          WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt AND t.deal_ymd = :ym
          GROUP BY t.building_name
          ORDER BY avgAmount DESC
          """,
      nativeQuery = true)
  List<ComplexRankRow> complexRanking(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("ym") String ym);

  /**
   * Same-complex change: (building, area-band) cells traded in BOTH {@code fromYm} and {@code toYm},
   * with each cell's avg 단위면적가 (만원/㎡) per month. Controls composition bias — only matched
   * 건물+평형대 are compared. Service computes the % change per cell.
   */
  @Query(
      value =
          """
          SELECT t.building_name AS buildingName,
                 CASE WHEN t.area <= 60 THEN 'SMALL'
                      WHEN t.area <= 85 THEN 'MID_SMALL'
                      WHEN t.area <= 135 THEN 'MID_LARGE'
                      ELSE 'LARGE' END AS band,
                 AVG(CASE WHEN t.deal_ymd = :fromYm THEN COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) END) AS fromAvg,
                 AVG(CASE WHEN t.deal_ymd = :toYm   THEN COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) END) AS toAvg
          FROM real_estate_transaction t
          WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt
            AND t.building_name IS NOT NULL
            AND t.deal_ymd IN (:fromYm, :toYm)
          GROUP BY t.building_name,
                 CASE WHEN t.area <= 60 THEN 'SMALL'
                      WHEN t.area <= 85 THEN 'MID_SMALL'
                      WHEN t.area <= 135 THEN 'MID_LARGE'
                      ELSE 'LARGE' END
          HAVING COUNT(CASE WHEN t.deal_ymd = :fromYm THEN 1 END) > 0
             AND COUNT(CASE WHEN t.deal_ymd = :toYm THEN 1 END) > 0
          """,
      nativeQuery = true)
  List<ComplexChangeRow> complexChange(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("fromYm") String fromYm,
      @Param("toYm") String toYm);

  @Query(
      "SELECT MAX(t.dealYmd) FROM RealEstateTransaction t "
          + "WHERE t.lawdCd = :lawdCd AND t.propertyType = :pt AND t.tradeType = :tt")
  String latestYmd(
      @Param("lawdCd") String lawdCd,
      @Param("pt") PropertyType propertyType,
      @Param("tt") TradeType tradeType);

  @Query(
      "SELECT COUNT(DISTINCT t.dealYmd) FROM RealEstateTransaction t "
          + "WHERE t.lawdCd = :lawdCd AND t.propertyType = :pt AND t.tradeType = :tt")
  long countMonths(
      @Param("lawdCd") String lawdCd,
      @Param("pt") PropertyType propertyType,
      @Param("tt") TradeType tradeType);

  /** Average primary amount (만원) for a region+month+type — for alert evaluation. */
  @Query(
      "SELECT AVG(COALESCE(t.dealAmount, t.deposit)) FROM RealEstateTransaction t "
          + "WHERE t.lawdCd = :lawdCd AND t.propertyType = :pt AND t.tradeType = :tt AND t.dealYmd = :ym")
  Double avgPrimaryByRegionAndYm(
      @Param("lawdCd") String lawdCd,
      @Param("pt") PropertyType propertyType,
      @Param("tt") TradeType tradeType,
      @Param("ym") String ym);

  @Query(
      "SELECT AVG(COALESCE(t.dealAmount, t.deposit)) FROM RealEstateTransaction t "
          + "WHERE t.lawdCd = :lawdCd AND t.propertyType = :pt AND t.tradeType = :tt "
          + "AND t.buildingName = :buildingName AND t.dealYmd = :ym")
  Double avgPrimaryByComplexAndYm(
      @Param("lawdCd") String lawdCd,
      @Param("pt") PropertyType propertyType,
      @Param("tt") TradeType tradeType,
      @Param("buildingName") String buildingName,
      @Param("ym") String ym);
}
