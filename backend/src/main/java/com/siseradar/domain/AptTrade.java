package com.siseradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One apartment sale transaction (국토부 아파트 매매 실거래가).
 *
 * <p>{@code dealAmount} is stored in <b>만원</b> (the source unit) as a long; the
 * comma is the only thing stripped on the way in. Convert at the display edge.
 * {@code area} is kept in ㎡; 평 is derived (= area / 3.305785).
 */
@Entity
@Table(
    name = "apt_trade",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_apt_trade",
            columnNames = {"lawd_cd", "apt_name", "area", "floor", "deal_date", "deal_amount"}),
    indexes = {
      @Index(name = "idx_apt_trade_lawd_ymd", columnList = "lawd_cd,deal_ymd"),
      @Index(name = "idx_apt_trade_apt_name", columnList = "apt_name")
    })
public class AptTrade {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 법정동 코드 앞 5자리 (= API의 sggCd). */
  @Column(name = "lawd_cd", nullable = false, length = 5)
  private String lawdCd;

  /** 계약 연월 YYYYMM. */
  @Column(name = "deal_ymd", nullable = false, length = 6)
  private String dealYmd;

  @Column(name = "apt_name", nullable = false)
  private String aptName;

  /** 법정동 이름 (= API의 umdNm), 표시용으로 비정규화 저장. */
  @Column(name = "umd_nm")
  private String umdNm;

  /** 전용면적 ㎡. */
  @Column(name = "area", nullable = false, precision = 8, scale = 2)
  private BigDecimal area;

  @Column(name = "floor", nullable = false)
  private int floor;

  @Column(name = "build_year")
  private Integer buildYear;

  /** 거래금액, 단위 만원. */
  @Column(name = "deal_amount", nullable = false)
  private long dealAmount;

  @Column(name = "jibun")
  private String jibun;

  @Column(name = "deal_date", nullable = false)
  private LocalDate dealDate;

  protected AptTrade() {}

  public AptTrade(
      String lawdCd,
      String dealYmd,
      String aptName,
      String umdNm,
      BigDecimal area,
      int floor,
      Integer buildYear,
      long dealAmount,
      String jibun,
      LocalDate dealDate) {
    this.lawdCd = lawdCd;
    this.dealYmd = dealYmd;
    this.aptName = aptName;
    this.umdNm = umdNm;
    this.area = area;
    this.floor = floor;
    this.buildYear = buildYear;
    this.dealAmount = dealAmount;
    this.jibun = jibun;
    this.dealDate = dealDate;
  }

  /** Stable natural key matching the DB unique constraint — used for idempotent inserts. */
  public String naturalKey() {
    return String.join(
        "|",
        lawdCd,
        aptName,
        area.toPlainString(),
        String.valueOf(floor),
        dealDate.toString(),
        String.valueOf(dealAmount));
  }

  public Long getId() {
    return id;
  }

  public String getLawdCd() {
    return lawdCd;
  }

  public String getDealYmd() {
    return dealYmd;
  }

  public String getAptName() {
    return aptName;
  }

  public String getUmdNm() {
    return umdNm;
  }

  public BigDecimal getArea() {
    return area;
  }

  public int getFloor() {
    return floor;
  }

  public Integer getBuildYear() {
    return buildYear;
  }

  public long getDealAmount() {
    return dealAmount;
  }

  public String getJibun() {
    return jibun;
  }

  public LocalDate getDealDate() {
    return dealDate;
  }
}
