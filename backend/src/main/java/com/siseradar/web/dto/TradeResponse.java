package com.siseradar.web.dto;

import com.siseradar.domain.AptTrade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** A single transaction for the client. {@code dealAmount} is 만원; 평 is derived from ㎡. */
public record TradeResponse(
    Long id,
    String aptName,
    String umdNm,
    String jibun,
    BigDecimal area,
    BigDecimal areaPyeong,
    int floor,
    Integer buildYear,
    long dealAmount,
    LocalDate dealDate) {

  private static final BigDecimal PYEONG = new BigDecimal("3.305785");

  public static TradeResponse from(AptTrade t) {
    BigDecimal pyeong = t.getArea().divide(PYEONG, 2, RoundingMode.HALF_UP);
    return new TradeResponse(
        t.getId(),
        t.getAptName(),
        t.getUmdNm(),
        t.getJibun(),
        t.getArea(),
        pyeong,
        t.getFloor(),
        t.getBuildYear(),
        t.getDealAmount(),
        t.getDealDate());
  }
}
