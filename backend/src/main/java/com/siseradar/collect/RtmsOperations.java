package com.siseradar.collect;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import java.util.List;
import java.util.Map;

/**
 * Maps (propertyType, tradeType) to its data.go.kr operation path under base 1613000.
 * {@link #ENABLED} is the set actually collected today — schema supports all RTMS types, but we
 * roll out collection incrementally (1차: 아파트 매매; 전월세는 다음 증분에서 ENABLED에 추가).
 */
public final class RtmsOperations {

  private RtmsOperations() {}

  public record TypePair(PropertyType propertyType, TradeType tradeType) {}

  private static final Map<TypePair, String> OPS =
      Map.of(
          new TypePair(PropertyType.APT, TradeType.SALE),
              "RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade",
          new TypePair(PropertyType.APT, TradeType.RENT),
              "RTMSDataSvcAptRent/getRTMSDataSvcAptRent");

  /** Currently collected combinations. */
  public static final List<TypePair> ENABLED = List.of(new TypePair(PropertyType.APT, TradeType.SALE));

  public static String operationPath(PropertyType propertyType, TradeType tradeType) {
    String op = OPS.get(new TypePair(propertyType, tradeType));
    if (op == null) {
      throw new IllegalArgumentException(
          "지원하지 않는 수집 조합: " + propertyType + "/" + tradeType);
    }
    return op;
  }
}
