package com.siseradar.collect;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import java.util.List;
import java.util.Map;

/**
 * Maps (propertyType, tradeType) to its data.go.kr operation path under base 1613000.
 * Path pattern: {@code RTMSDataSvc<Op>/getRTMSDataSvc<Op>}.
 */
public final class RtmsOperations {

  private RtmsOperations() {}

  public record TypePair(PropertyType propertyType, TradeType tradeType) {}

  private static String path(String op) {
    return "RTMSDataSvc" + op + "/getRTMSDataSvc" + op;
  }

  private static final Map<TypePair, String> OPS =
      Map.ofEntries(
          Map.entry(new TypePair(PropertyType.APT, TradeType.SALE), path("AptTrade")),
          Map.entry(new TypePair(PropertyType.APT, TradeType.RENT), path("AptRent")),
          Map.entry(new TypePair(PropertyType.OFFICETEL, TradeType.SALE), path("OffiTrade")),
          Map.entry(new TypePair(PropertyType.OFFICETEL, TradeType.RENT), path("OffiRent")),
          Map.entry(new TypePair(PropertyType.ROW_HOUSE, TradeType.SALE), path("RHTrade")),
          Map.entry(new TypePair(PropertyType.ROW_HOUSE, TradeType.RENT), path("RHRent")),
          Map.entry(new TypePair(PropertyType.DETACHED, TradeType.SALE), path("SHTrade")),
          Map.entry(new TypePair(PropertyType.DETACHED, TradeType.RENT), path("SHRent")),
          Map.entry(new TypePair(PropertyType.COMMERCIAL, TradeType.SALE), path("NrgTrade")),
          Map.entry(new TypePair(PropertyType.LAND, TradeType.SALE), path("LandTrade")),
          Map.entry(new TypePair(PropertyType.INDUSTRIAL, TradeType.SALE), path("InduTrade")),
          Map.entry(new TypePair(PropertyType.PRESALE_RIGHT, TradeType.SALE), path("SilvTrade")));

  /**
   * Combinations collected by the scheduler / on-demand backfill (APT first → status flips early).
   * 전유형 수집 — 대시보드는 유형별 평당가·거래량·추세를 모두 보여준다(파서가 유형별 면적을 채움).
   * 단지명 없는 유형(단독·상업·토지·산업)은 동일단지·랭킹·지도 마커가 자동 제외되고(hasRanking),
   * 지도 선택자에서도 빠진다.
   */
  public static final List<TypePair> ENABLED =
      List.of(
          new TypePair(PropertyType.APT, TradeType.SALE),
          new TypePair(PropertyType.APT, TradeType.RENT),
          new TypePair(PropertyType.OFFICETEL, TradeType.SALE),
          new TypePair(PropertyType.OFFICETEL, TradeType.RENT),
          new TypePair(PropertyType.ROW_HOUSE, TradeType.SALE),
          new TypePair(PropertyType.ROW_HOUSE, TradeType.RENT),
          new TypePair(PropertyType.DETACHED, TradeType.SALE),
          new TypePair(PropertyType.DETACHED, TradeType.RENT),
          new TypePair(PropertyType.COMMERCIAL, TradeType.SALE),
          new TypePair(PropertyType.LAND, TradeType.SALE),
          new TypePair(PropertyType.INDUSTRIAL, TradeType.SALE),
          new TypePair(PropertyType.PRESALE_RIGHT, TradeType.SALE));

  public static String operationPath(PropertyType propertyType, TradeType tradeType) {
    String op = OPS.get(new TypePair(propertyType, tradeType));
    if (op == null) {
      throw new IllegalArgumentException("지원하지 않는 수집 조합: " + propertyType + "/" + tradeType);
    }
    return op;
  }
}
