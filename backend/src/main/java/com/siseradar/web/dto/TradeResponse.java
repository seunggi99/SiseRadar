package com.siseradar.web.dto;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.RealEstateTransaction;
import com.siseradar.domain.TradeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * A single transaction for the client. {@code aptName} is the building name (kept for frontend
 * back-compat). SALE uses {@code dealAmount}; RENT uses {@code deposit}+{@code monthlyRent} (만원).
 */
public record TradeResponse(
    Long id,
    PropertyType propertyType,
    TradeType tradeType,
    String aptName,
    String umdNm,
    String jibun,
    BigDecimal area,
    BigDecimal areaPyeong,
    Integer floor,
    Integer buildYear,
    Long dealAmount,
    Long deposit,
    Integer monthlyRent,
    LocalDate dealDate) {

  private static final BigDecimal PYEONG = new BigDecimal("3.305785");

  public static TradeResponse from(RealEstateTransaction t) {
    BigDecimal pyeong =
        t.getArea() == null ? null : t.getArea().divide(PYEONG, 2, RoundingMode.HALF_UP);
    return new TradeResponse(
        t.getId(),
        t.getPropertyType(),
        t.getTradeType(),
        t.getBuildingName(),
        t.getUmdNm(),
        t.getJibun(),
        t.getArea(),
        pyeong,
        t.getFloor(),
        t.getBuildYear(),
        t.getDealAmount(),
        t.getDeposit(),
        t.getMonthlyRent(),
        t.getDealDate());
  }
}
