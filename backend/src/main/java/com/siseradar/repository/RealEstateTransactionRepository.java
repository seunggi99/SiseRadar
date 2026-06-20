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
          SELECT b.ym AS ym,
                 COUNT(*) AS cnt,
                 COUNT(DISTINCT b.deal_ymd) AS monthsInBucket,
                 AVG(b.amt) AS avgAmount,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY b.amt) AS medianAmount,
                 AVG(b.ppa) AS avgPricePerArea,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY b.ppa) AS medianPricePerArea,
                 AVG(b.monthly_rent) AS avgMonthlyRent
          FROM (
            SELECT t.deal_ymd AS deal_ymd,
                   CAST(CAST(LEFT(t.deal_ymd, 4) AS INT) * 100
                        + ((CAST(RIGHT(t.deal_ymd, 2) AS INT) - 1) / :bucketMonths * :bucketMonths + 1) AS VARCHAR) AS ym,
                   COALESCE(t.deal_amount, t.deposit) AS amt,
                   COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) AS ppa,
                   t.monthly_rent AS monthly_rent
            FROM real_estate_transaction t
            WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt
              AND (:from IS NULL OR t.deal_ymd >= :from)
              AND (:to IS NULL OR t.deal_ymd <= :to)
          ) b
          GROUP BY b.ym
          ORDER BY b.ym
          """,
      nativeQuery = true)
  List<MonthlyStatRow> monthlyStats(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("from") String from,
      @Param("to") String to,
      @Param("bucketMonths") int bucketMonths);

  /**
   * Per-month, per-area-band breakdown (전용면적 기준 구간). Bands: ≤60 / 60–85 / 85–135 / >135.
   * Rows with null area land in LARGE (rare; residential types always have area).
   */
  @Query(
      value =
          """
          SELECT b.ym AS ym,
                 b.band AS band,
                 COUNT(*) AS cnt,
                 AVG(b.ppa) AS avgPricePerArea,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY b.ppa) AS medianPricePerArea
          FROM (
            SELECT CAST(CAST(LEFT(t.deal_ymd, 4) AS INT) * 100
                        + ((CAST(RIGHT(t.deal_ymd, 2) AS INT) - 1) / :bucketMonths * :bucketMonths + 1) AS VARCHAR) AS ym,
                   CASE WHEN t.area <= 60 THEN 'SMALL'
                        WHEN t.area <= 85 THEN 'MID_SMALL'
                        WHEN t.area <= 135 THEN 'MID_LARGE'
                        ELSE 'LARGE' END AS band,
                   COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) AS ppa
            FROM real_estate_transaction t
            WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt
              AND (:from IS NULL OR t.deal_ymd >= :from)
              AND (:to IS NULL OR t.deal_ymd <= :to)
          ) b
          GROUP BY b.ym, b.band
          ORDER BY b.ym
          """,
      nativeQuery = true)
  List<BandStatRow> monthlyStatsByBand(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("from") String from,
      @Param("to") String to,
      @Param("bucketMonths") int bucketMonths);

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

  /**
   * Per-building stats for the map over a period (전용 단위면적가 만원/㎡), optional 평형대 필터.
   * building_name 있는 단지만. umdNm(대표 동)은 지오코딩 질의에 사용.
   */
  @Query(
      value =
          """
          SELECT t.building_name AS buildingName,
                 MAX(t.umd_nm) AS umdNm,
                 COUNT(*) AS cnt,
                 AVG(COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS avgPricePerArea,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS medianPricePerArea
          FROM real_estate_transaction t
          WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt
            AND t.building_name IS NOT NULL
            AND (:from IS NULL OR t.deal_ymd >= :from)
            AND (:to IS NULL OR t.deal_ymd <= :to)
            AND (:band IS NULL OR
                 CASE WHEN t.area <= 60 THEN 'SMALL'
                      WHEN t.area <= 85 THEN 'MID_SMALL'
                      WHEN t.area <= 135 THEN 'MID_LARGE'
                      ELSE 'LARGE' END = :band)
          GROUP BY t.building_name
          """,
      nativeQuery = true)
  List<MapComplexStatRow> mapComplexStats(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("from") String from,
      @Param("to") String to,
      @Param("band") String band);

  /**
   * Per-region aggregate over ALL matching transactions (지오코딩 무관 — 전체 거래) for low-zoom
   * bubbles. 전용 단위면적가 만원/㎡. from/to null = 전체 기간(대시보드와 동일).
   */
  @Query(
      value =
          """
          SELECT t.lawd_cd AS lawdCd,
                 COUNT(*) AS cnt,
                 AVG(COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS avgPricePerArea,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS medianPricePerArea
          FROM real_estate_transaction t
          WHERE t.property_type = :pt AND t.trade_type = :tt
            AND (:from IS NULL OR t.deal_ymd >= :from)
            AND (:to IS NULL OR t.deal_ymd <= :to)
            AND (:band IS NULL OR
                 CASE WHEN t.area <= 60 THEN 'SMALL'
                      WHEN t.area <= 85 THEN 'MID_SMALL'
                      WHEN t.area <= 135 THEN 'MID_LARGE'
                      ELSE 'LARGE' END = :band)
          GROUP BY t.lawd_cd
          """,
      nativeQuery = true)
  List<MapRegionStatRow> regionStats(
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("from") String from,
      @Param("to") String to,
      @Param("band") String band);

  @Query(
      "SELECT MAX(t.dealYmd) FROM RealEstateTransaction t "
          + "WHERE t.lawdCd = :lawdCd AND t.propertyType = :pt AND t.tradeType = :tt")
  String latestYmd(
      @Param("lawdCd") String lawdCd,
      @Param("pt") PropertyType propertyType,
      @Param("tt") TradeType tradeType);

  /** Latest 거래월 over ALL regions for a type — the global anchor for the map's 1년 변동률 windows. */
  @Query(
      "SELECT MAX(t.dealYmd) FROM RealEstateTransaction t "
          + "WHERE t.propertyType = :pt AND t.tradeType = :tt")
  String globalLatestYmd(@Param("pt") PropertyType propertyType, @Param("tt") TradeType tradeType);

  /**
   * Markers for a viewport bbox: geocoded complexes whose own coordinates fall inside the box
   * (joined with 전용 단위면적가 stats). Selecting by the building's coordinate — not the region
   * centroid — means markers render at ANY zoom, including max zoom-in. Also returns the current /
   * prior 12-month avg + count (global windows) so the caller can compute each complex's 1년 변동률.
   */
  @Query(
      value =
          """
          SELECT g.lawd_cd AS lawdCd, t.building_name AS buildingName,
                 g.lat AS lat, g.lng AS lng,
                 COUNT(*) AS cnt,
                 AVG(COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS avgPricePerArea,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS medianPricePerArea,
                 AVG(CASE WHEN t.deal_ymd BETWEEN :curFrom AND :curTo THEN COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) END) AS curAvg,
                 AVG(CASE WHEN t.deal_ymd BETWEEN :prevFrom AND :prevTo THEN COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) END) AS prevAvg,
                 SUM(CASE WHEN t.deal_ymd BETWEEN :curFrom AND :curTo THEN 1 ELSE 0 END) AS curCnt,
                 SUM(CASE WHEN t.deal_ymd BETWEEN :prevFrom AND :prevTo THEN 1 ELSE 0 END) AS prevCnt
          FROM real_estate_transaction t
          JOIN complex_geocode g
            ON g.lawd_cd = t.lawd_cd AND g.building_name = t.building_name
               AND g.property_type = t.property_type
          WHERE g.status = 'SUCCESS' AND g.lat IS NOT NULL AND g.lng IS NOT NULL
            AND g.lat BETWEEN :swLat AND :neLat AND g.lng BETWEEN :swLng AND :neLng
            AND t.property_type = :pt AND t.trade_type = :tt
            AND (:from IS NULL OR t.deal_ymd >= :from)
            AND (:to IS NULL OR t.deal_ymd <= :to)
            AND (:band IS NULL OR
                 CASE WHEN t.area <= 60 THEN 'SMALL'
                      WHEN t.area <= 85 THEN 'MID_SMALL'
                      WHEN t.area <= 135 THEN 'MID_LARGE'
                      ELSE 'LARGE' END = :band)
          GROUP BY g.lawd_cd, t.building_name, g.lat, g.lng
          """,
      nativeQuery = true)
  List<MapMarkerRow> markersInBbox(
      @Param("swLat") double swLat,
      @Param("neLat") double neLat,
      @Param("swLng") double swLng,
      @Param("neLng") double neLng,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("from") String from,
      @Param("to") String to,
      @Param("band") String band,
      @Param("curFrom") String curFrom,
      @Param("curTo") String curTo,
      @Param("prevFrom") String prevFrom,
      @Param("prevTo") String prevTo);

  /**
   * Per-region 동일단지(same-store) 평균 1년 변동률: for each building present in BOTH the current
   * and prior 12-month windows, the % change of its 전용 단위면적가, averaged within the region
   * (구성 편향 통제). Regions with no matched same-store building are absent → bubble shows 데이터
   * 부족, not 0%.
   */
  @Query(
      value =
          """
          SELECT sub.lawd_cd AS lawdCd,
                 AVG((sub.cur - sub.prev) / sub.prev * 100) AS changePct,
                 COUNT(*) AS matched
          FROM (
            SELECT t.lawd_cd AS lawd_cd, t.building_name AS bn,
                   AVG(CASE WHEN t.deal_ymd BETWEEN :curFrom AND :curTo THEN COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) END) AS cur,
                   AVG(CASE WHEN t.deal_ymd BETWEEN :prevFrom AND :prevTo THEN COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) END) AS prev
            FROM real_estate_transaction t
            WHERE t.property_type = :pt AND t.trade_type = :tt AND t.building_name IS NOT NULL
              AND (:band IS NULL OR
                   CASE WHEN t.area <= 60 THEN 'SMALL'
                        WHEN t.area <= 85 THEN 'MID_SMALL'
                        WHEN t.area <= 135 THEN 'MID_LARGE'
                        ELSE 'LARGE' END = :band)
            GROUP BY t.lawd_cd, t.building_name
          ) sub
          WHERE sub.cur IS NOT NULL AND sub.prev IS NOT NULL AND sub.prev <> 0
          GROUP BY sub.lawd_cd
          """,
      nativeQuery = true)
  List<RegionChangeRow> regionChange(
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("band") String band,
      @Param("curFrom") String curFrom,
      @Param("curTo") String curTo,
      @Param("prevFrom") String prevFrom,
      @Param("prevTo") String prevTo);

  /**
   * One region's 동일단지(same-store) 변동률 over a fixed current/prior 12-month window — the SINGLE
   * canonical 변동률 calc shared by the map bubble, dashboard card, and AI summary. Per building:
   * avg 전용 단위면적가 in each window; keep buildings present in BOTH; region avg + median of the
   * per-building % change, and the matched-building count. matched=0 → 데이터 부족 (no forced 0%).
   */
  @Query(
      value =
          """
          SELECT AVG((sub.cur - sub.prev) / sub.prev * 100) AS avgPct,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY (sub.cur - sub.prev) / sub.prev * 100) AS medianPct,
                 COUNT(*) AS matched
          FROM (
            SELECT t.building_name AS bn,
                   AVG(CASE WHEN t.deal_ymd BETWEEN :curFrom AND :curTo THEN COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) END) AS cur,
                   AVG(CASE WHEN t.deal_ymd BETWEEN :prevFrom AND :prevTo THEN COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0) END) AS prev
            FROM real_estate_transaction t
            WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt
              AND t.building_name IS NOT NULL
            GROUP BY t.building_name
          ) sub
          WHERE sub.cur IS NOT NULL AND sub.prev IS NOT NULL AND sub.prev <> 0
          """,
      nativeQuery = true)
  SameStoreChangeRow sameStoreChange12(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("curFrom") String curFrom,
      @Param("curTo") String curTo,
      @Param("prevFrom") String prevFrom,
      @Param("prevTo") String prevTo);

  /** Latest 거래월 for one building — the anchor for its 변동률 windows. */
  @Query(
      "SELECT MAX(t.dealYmd) FROM RealEstateTransaction t "
          + "WHERE t.lawdCd = :lawdCd AND t.propertyType = :pt AND t.tradeType = :tt "
          + "AND t.buildingName = :bn")
  String complexLatestYmd(
      @Param("lawdCd") String lawdCd,
      @Param("pt") PropertyType propertyType,
      @Param("tt") TradeType tradeType,
      @Param("bn") String buildingName);

  /**
   * 단위면적가(전용, 만원/㎡) 평균 + 거래 건수 for ONE building over a [from,to] window (optional
   * 평형대). Aggregate w/o GROUP BY → always one row; cnt=0 / avg=null when no transactions.
   */
  @Query(
      value =
          """
          SELECT AVG(COALESCE(t.deal_amount, t.deposit) / NULLIF(t.area, 0)) AS avgPricePerArea,
                 COUNT(*) AS cnt
          FROM real_estate_transaction t
          WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.trade_type = :tt
            AND t.building_name = :bn
            AND t.deal_ymd >= :from AND t.deal_ymd <= :to
            AND (:band IS NULL OR
                 CASE WHEN t.area <= 60 THEN 'SMALL'
                      WHEN t.area <= 85 THEN 'MID_SMALL'
                      WHEN t.area <= 135 THEN 'MID_LARGE'
                      ELSE 'LARGE' END = :band)
          """,
      nativeQuery = true)
  ComplexPeriodRow complexPeriodStat(
      @Param("lawdCd") String lawdCd,
      @Param("pt") String propertyType,
      @Param("tt") String tradeType,
      @Param("bn") String buildingName,
      @Param("from") String from,
      @Param("to") String to,
      @Param("band") String band);

  @Query(
      "SELECT COUNT(DISTINCT t.dealYmd) FROM RealEstateTransaction t "
          + "WHERE t.lawdCd = :lawdCd AND t.propertyType = :pt AND t.tradeType = :tt")
  long countMonths(
      @Param("lawdCd") String lawdCd,
      @Param("pt") PropertyType propertyType,
      @Param("tt") TradeType tradeType);

  /**
   * Distinct building names (+대표 동) for one (region, propertyType) across all trades — used to
   * eagerly warm the geocode cache so markers render in one shot instead of trickling in.
   */
  @Query(
      value =
          """
          SELECT t.building_name AS buildingName, MAX(t.umd_nm) AS umdNm
          FROM real_estate_transaction t
          WHERE t.lawd_cd = :lawdCd AND t.property_type = :pt AND t.building_name IS NOT NULL
          GROUP BY t.building_name
          """,
      nativeQuery = true)
  List<BuildingRow> distinctBuildings(@Param("lawdCd") String lawdCd, @Param("pt") String propertyType);

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
