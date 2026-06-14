package com.siseradar.collect;

import com.siseradar.alert.AlertEvaluationService;
import com.siseradar.collect.dto.RtmsApiResponse;
import com.siseradar.domain.PropertyType;
import com.siseradar.domain.RealEstateTransaction;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.RealEstateTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fetches every page for a region+month+type and persists new transactions idempotently:
 * existing dedup keys for that (region, month, type) are loaded once, and only unseen rows are
 * inserted, so re-runs are safe.
 */
@Service
public class TradeCollectionService {

  private static final Logger log = LoggerFactory.getLogger(TradeCollectionService.class);

  private final DataGoKrClient client;
  private final RealEstateTransactionRepository repository;
  private final CollectionProperties props;
  private final AlertEvaluationService alertEvaluation;

  public TradeCollectionService(
      DataGoKrClient client,
      RealEstateTransactionRepository repository,
      CollectionProperties props,
      AlertEvaluationService alertEvaluation) {
    this.client = client;
    this.repository = repository;
    this.props = props;
    this.alertEvaluation = alertEvaluation;
  }

  public record Result(
      String lawdCd,
      PropertyType propertyType,
      TradeType tradeType,
      String dealYmd,
      int fetched,
      int inserted,
      int skipped) {}

  @Transactional
  public Result collect(
      String lawdCd, String dealYmd, PropertyType propertyType, TradeType tradeType) {
    String operationPath = RtmsOperations.operationPath(propertyType, tradeType);

    Set<String> existing = new HashSet<>();
    repository
        .findByLawdCdAndDealYmdAndPropertyTypeAndTradeType(lawdCd, dealYmd, propertyType, tradeType)
        .forEach(t -> existing.add(t.getDedupKey()));

    Set<String> seenThisRun = new HashSet<>();
    List<RealEstateTransaction> toInsert = new ArrayList<>();
    int fetched = 0;
    int numOfRows = props.numOfRows() > 0 ? props.numOfRows() : 100;

    int pageNo = 1;
    while (true) {
      RtmsApiResponse res = client.fetch(operationPath, lawdCd, dealYmd, pageNo, numOfRows);
      List<RtmsApiResponse.Item> items =
          res.body == null || res.body.items == null || res.body.items.item == null
              ? List.of()
              : res.body.items.item;

      for (RtmsApiResponse.Item item : items) {
        fetched++;
        RealEstateTransaction tx = toEntity(lawdCd, dealYmd, propertyType, tradeType, item);
        if (tx == null) {
          continue;
        }
        String key = tx.getDedupKey();
        if (existing.contains(key) || !seenThisRun.add(key)) {
          continue;
        }
        toInsert.add(tx);
      }

      int totalCount = res.body == null ? 0 : res.body.totalCount;
      if (items.isEmpty() || (long) pageNo * numOfRows >= totalCount) {
        break;
      }
      pageNo++;
      sleep(props.pageDelayMs());
    }

    repository.saveAll(toInsert);

    try {
      alertEvaluation.evaluate(lawdCd, dealYmd, propertyType, tradeType, toInsert);
    } catch (RuntimeException e) {
      log.warn("Alert evaluation failed for {} {} {}/{}: {}", lawdCd, dealYmd, propertyType, tradeType, e.getMessage());
    }

    Result result =
        new Result(
            lawdCd, propertyType, tradeType, dealYmd, fetched, toInsert.size(),
            fetched - toInsert.size());
    log.info(
        "Collected {} {} {}/{}: fetched={} inserted={} skipped={}",
        lawdCd, dealYmd, propertyType, tradeType, fetched, result.inserted(), result.skipped());
    return result;
  }

  private RealEstateTransaction toEntity(
      String lawdCd,
      String dealYmd,
      PropertyType propertyType,
      TradeType tradeType,
      RtmsApiResponse.Item item) {
    try {
      String areaRaw = areaFieldFor(propertyType, item);
      BigDecimal area = areaRaw == null || areaRaw.isBlank() ? null : new BigDecimal(areaRaw.trim());
      Integer floor = parseNullableInt(item.floor);
      Integer buildYear = parseNullableInt(item.buildYear);
      LocalDate dealDate =
          LocalDate.of(
              Integer.parseInt(item.dealYear.trim()),
              Integer.parseInt(item.dealMonth.trim()),
              Integer.parseInt(item.dealDay.trim()));
      String buildingName = trimOrNull(buildingNameFor(propertyType, item));
      String umdNm = item.umdNm == null ? null : item.umdNm.trim();
      String jibun = item.jibun == null ? null : item.jibun.trim();

      Long dealAmount = null;
      Long deposit = null;
      Integer monthlyRent = null;
      if (tradeType == TradeType.SALE) {
        dealAmount = parseManwon(item.dealAmount);
      } else {
        deposit = parseManwon(item.deposit);
        Long mr = parseManwon(item.monthlyRent);
        monthlyRent = mr == null ? 0 : mr.intValue();
      }

      return new RealEstateTransaction(
          propertyType, tradeType, lawdCd, dealYmd, buildingName, umdNm, area, floor, buildYear,
          dealAmount, deposit, monthlyRent, jibun, dealDate);
    } catch (RuntimeException e) {
      log.warn(
          "Skipping unparseable item ({} {} {}/{}): {}",
          lawdCd, dealYmd, propertyType, tradeType, e.getMessage());
      return null;
    }
  }

  /** Building-name field varies by type; null for types without a building (단독/상업/토지/산업). */
  private static String buildingNameFor(PropertyType pt, RtmsApiResponse.Item item) {
    return switch (pt) {
      case APT, PRESALE_RIGHT -> item.aptNm;
      case OFFICETEL -> item.offiNm;
      case ROW_HOUSE -> item.mhouseNm;
      case DETACHED, COMMERCIAL, LAND, INDUSTRIAL -> null;
    };
  }

  /** Area field varies by type: 전용면적/연면적/건물면적/거래면적. */
  private static String areaFieldFor(PropertyType pt, RtmsApiResponse.Item item) {
    return switch (pt) {
      case APT, OFFICETEL, ROW_HOUSE, PRESALE_RIGHT -> item.excluUseAr;
      case DETACHED -> item.totalFloorAr;
      case COMMERCIAL, INDUSTRIAL -> item.buildingAr;
      case LAND -> item.dealArea;
    };
  }

  private static String trimOrNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  /** Strips commas, parses 만원 → Long; null/blank → null. */
  private static Long parseManwon(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return Long.parseLong(raw.replace(",", "").trim());
  }

  private static Integer parseNullableInt(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static void sleep(long ms) {
    if (ms <= 0) {
      return;
    }
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
