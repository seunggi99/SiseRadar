package com.siseradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * One RTMS real-estate transaction across all property/trade types.
 *
 * <p>Money is stored in <b>만원</b>: {@code dealAmount} for SALE; {@code deposit}+{@code monthlyRent}
 * for RENT (monthlyRent 0 = 전세). The "primary amount" used in aggregations is
 * {@code COALESCE(deal_amount, deposit)}. Dedup is via a single stored {@link #dedupKey} (UNIQUE),
 * which sidesteps NULL-distinctness and BigDecimal-scale pitfalls of a wide multi-column key.
 */
@Entity
@Table(
    name = "real_estate_transaction",
    uniqueConstraints = @UniqueConstraint(name = "uk_ret_dedup", columnNames = "dedup_key"),
    indexes = {
      @Index(name = "idx_ret_main", columnList = "lawd_cd,property_type,trade_type,deal_ymd"),
      @Index(name = "idx_ret_building", columnList = "building_name")
    })
public class RealEstateTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "property_type", nullable = false, length = 20)
  private PropertyType propertyType;

  @Enumerated(EnumType.STRING)
  @Column(name = "trade_type", nullable = false, length = 8)
  private TradeType tradeType;

  @Column(name = "lawd_cd", nullable = false, length = 5)
  private String lawdCd;

  @Column(name = "deal_ymd", nullable = false, length = 6)
  private String dealYmd;

  /** 단지명/건물명 (토지·단독은 null일 수 있음). */
  @Column(name = "building_name")
  private String buildingName;

  /** 법정동 이름 (표시용 비정규화). */
  @Column(name = "umd_nm")
  private String umdNm;

  /** 전용면적 ㎡ (유형별 면적 의미 차이는 문서화). */
  @Column(name = "area", precision = 10, scale = 2)
  private BigDecimal area;

  @Column(name = "floor")
  private Integer floor;

  @Column(name = "build_year")
  private Integer buildYear;

  /** 매매·토지·상업·분양 거래금액 (만원). */
  @Column(name = "deal_amount")
  private Long dealAmount;

  /** 전월세 보증금 (만원). */
  @Column(name = "deposit")
  private Long deposit;

  /** 전월세 월세 (만원, 0=전세). */
  @Column(name = "monthly_rent")
  private Integer monthlyRent;

  @Column(name = "jibun")
  private String jibun;

  @Column(name = "deal_date", nullable = false)
  private LocalDate dealDate;

  @Column(name = "dedup_key", nullable = false, length = 320)
  private String dedupKey;

  protected RealEstateTransaction() {}

  public RealEstateTransaction(
      PropertyType propertyType,
      TradeType tradeType,
      String lawdCd,
      String dealYmd,
      String buildingName,
      String umdNm,
      BigDecimal area,
      Integer floor,
      Integer buildYear,
      Long dealAmount,
      Long deposit,
      Integer monthlyRent,
      String jibun,
      LocalDate dealDate) {
    this.propertyType = propertyType;
    this.tradeType = tradeType;
    this.lawdCd = lawdCd;
    this.dealYmd = dealYmd;
    this.buildingName = buildingName;
    this.umdNm = umdNm;
    this.area = area == null ? null : area.setScale(2, RoundingMode.HALF_UP);
    this.floor = floor;
    this.buildYear = buildYear;
    this.dealAmount = dealAmount;
    this.deposit = deposit;
    this.monthlyRent = monthlyRent;
    this.jibun = jibun;
    this.dealDate = dealDate;
    this.dedupKey = buildDedupKey();
  }

  /**
   * Deterministic dedup key across all identifying fields, null-safe. Whichever amount fields are
   * set (deal_amount for SALE, deposit/monthly_rent for RENT) participate; nulls become "-".
   */
  private String buildDedupKey() {
    return String.join(
        "|",
        propertyType.name(),
        tradeType.name(),
        lawdCd,
        dealDate.toString(),
        nz(umdNm),
        nz(jibun),
        nz(buildingName),
        area == null ? "-" : area.toPlainString(),
        floor == null ? "-" : floor.toString(),
        dealAmount == null ? "-" : dealAmount.toString(),
        deposit == null ? "-" : deposit.toString(),
        monthlyRent == null ? "-" : monthlyRent.toString());
  }

  private static String nz(String s) {
    return s == null || s.isBlank() ? "-" : s;
  }

  public Long getId() {
    return id;
  }

  public PropertyType getPropertyType() {
    return propertyType;
  }

  public TradeType getTradeType() {
    return tradeType;
  }

  public String getLawdCd() {
    return lawdCd;
  }

  public String getDealYmd() {
    return dealYmd;
  }

  public String getBuildingName() {
    return buildingName;
  }

  public String getUmdNm() {
    return umdNm;
  }

  public BigDecimal getArea() {
    return area;
  }

  public Integer getFloor() {
    return floor;
  }

  public Integer getBuildYear() {
    return buildYear;
  }

  public Long getDealAmount() {
    return dealAmount;
  }

  public Long getDeposit() {
    return deposit;
  }

  public Integer getMonthlyRent() {
    return monthlyRent;
  }

  public String getJibun() {
    return jibun;
  }

  public LocalDate getDealDate() {
    return dealDate;
  }

  public String getDedupKey() {
    return dedupKey;
  }
}
