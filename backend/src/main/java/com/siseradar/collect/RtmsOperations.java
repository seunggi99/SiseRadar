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
   * 주거 3종(아파트·오피스텔·연립다세대) 매매+전월세만 — 단독다가구는 건물명이 없어 동일단지·
   * 지도 마커·랭킹에 못 쓰고, 상업/토지/산업/분양권은 활용도 대비 용량·수집비용이 커 제외.
   * (OPS 매핑 표에는 남겨두어 필요 시 다시 켤 수 있다.)
   */
  public static final List<TypePair> ENABLED =
      List.of(
          new TypePair(PropertyType.APT, TradeType.SALE),
          new TypePair(PropertyType.APT, TradeType.RENT),
          new TypePair(PropertyType.OFFICETEL, TradeType.SALE),
          new TypePair(PropertyType.OFFICETEL, TradeType.RENT),
          new TypePair(PropertyType.ROW_HOUSE, TradeType.SALE),
          new TypePair(PropertyType.ROW_HOUSE, TradeType.RENT));

  public static String operationPath(PropertyType propertyType, TradeType tradeType) {
    String op = OPS.get(new TypePair(propertyType, tradeType));
    if (op == null) {
      throw new IllegalArgumentException("지원하지 않는 수집 조합: " + propertyType + "/" + tradeType);
    }
    return op;
  }
}
