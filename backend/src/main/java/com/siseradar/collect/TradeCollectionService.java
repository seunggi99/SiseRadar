package com.siseradar.collect;

import com.siseradar.collect.dto.AptTradeApiResponse;
import com.siseradar.domain.AptTrade;
import com.siseradar.repository.AptTradeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * Fetches every page for a region+month and persists new transactions idempotently:
 * existing natural keys for that month are loaded once, and only unseen rows are inserted,
 * so re-runs (the most recent months keep gaining filings) are safe.
 */
@Service
public class TradeCollectionService {

  private static final Logger log = LoggerFactory.getLogger(TradeCollectionService.class);

  private final DataGoKrClient client;
  private final AptTradeRepository repository;
  private final CollectionProperties props;

  public TradeCollectionService(
      DataGoKrClient client, AptTradeRepository repository, CollectionProperties props) {
    this.client = client;
    this.repository = repository;
    this.props = props;
  }

  /** Result of one region+month collection run. */
  public record Result(String lawdCd, String dealYmd, int fetched, int inserted, int skipped) {}

  @Transactional
  public Result collect(String lawdCd, String dealYmd) {
    Set<String> existing = new HashSet<>();
    repository
        .findByLawdCdAndDealYmd(lawdCd, dealYmd)
        .forEach(t -> existing.add(t.naturalKey()));

    Set<String> seenThisRun = new HashSet<>();
    List<AptTrade> toInsert = new ArrayList<>();
    int fetched = 0;
    int numOfRows = props.numOfRows() > 0 ? props.numOfRows() : 100;

    int pageNo = 1;
    while (true) {
      AptTradeApiResponse res = client.fetch(lawdCd, dealYmd, pageNo, numOfRows);
      List<AptTradeApiResponse.Item> items =
          res.body == null || res.body.items == null || res.body.items.item == null
              ? List.of()
              : res.body.items.item;

      for (AptTradeApiResponse.Item item : items) {
        fetched++;
        AptTrade trade = toEntity(lawdCd, dealYmd, item);
        if (trade == null) {
          continue;
        }
        String key = trade.naturalKey();
        if (existing.contains(key) || !seenThisRun.add(key)) {
          continue;
        }
        toInsert.add(trade);
      }

      int totalCount = res.body == null ? 0 : res.body.totalCount;
      if (items.isEmpty() || (long) pageNo * numOfRows >= totalCount) {
        break;
      }
      pageNo++;
      sleep(props.pageDelayMs());
    }

    repository.saveAll(toInsert);
    Result result = new Result(lawdCd, dealYmd, fetched, toInsert.size(), fetched - toInsert.size());
    log.info(
        "Collected {} {}: fetched={} inserted={} skipped={}",
        lawdCd,
        dealYmd,
        result.fetched(),
        result.inserted(),
        result.skipped());
    return result;
  }

  private AptTrade toEntity(String lawdCd, String dealYmd, AptTradeApiResponse.Item item) {
    try {
      long amount = Long.parseLong(item.dealAmount.replace(",", "").trim());
      // Normalize to the DB column scale (2) so the natural key is stable across re-runs
      // (DB reloads as e.g. 131.40; a raw "131.4" parse would otherwise miss the dedup check).
      BigDecimal area = new BigDecimal(item.excluUseAr.trim()).setScale(2, RoundingMode.HALF_UP);
      int floor = Integer.parseInt(item.floor.trim());
      Integer buildYear = parseNullableInt(item.buildYear);
      LocalDate dealDate =
          LocalDate.of(
              Integer.parseInt(item.dealYear.trim()),
              Integer.parseInt(item.dealMonth.trim()),
              Integer.parseInt(item.dealDay.trim()));
      String aptName = item.aptNm == null ? "" : item.aptNm.trim();
      String umdNm = item.umdNm == null ? null : item.umdNm.trim();
      String jibun = item.jibun == null ? null : item.jibun.trim();
      return new AptTrade(
          lawdCd, dealYmd, aptName, umdNm, area, floor, buildYear, amount, jibun, dealDate);
    } catch (RuntimeException e) {
      log.warn("Skipping unparseable item ({} {}): {}", lawdCd, dealYmd, e.getMessage());
      return null;
    }
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
