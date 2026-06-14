package com.siseradar.collect;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.RealEstateTransactionRepository;
import org.springframework.stereotype.Service;

/** Decides whether a region needs on-demand collection and reports its status. */
@Service
public class RegionCollectionService {

  /** How many months to backfill the first time a region is opened. */
  private static final int BACKFILL_MONTHS = 24;

  private final RealEstateTransactionRepository trades;
  private final RegionCollectionWorker worker;

  public RegionCollectionService(
      RealEstateTransactionRepository trades, RegionCollectionWorker worker) {
    this.trades = trades;
    this.worker = worker;
  }

  public enum State {
    NONE, // never collected, not running
    COLLECTING, // backfill in progress
    DONE // has data (or finished)
  }

  public record Status(State state, long months) {}

  public Status status(String lawdCd) {
    // "Has data" is judged by the default dashboard view (아파트 매매).
    long months = trades.countMonths(lawdCd, PropertyType.APT, TradeType.SALE);
    if (months > 0) {
      return new Status(State.DONE, months);
    }
    return new Status(worker.isCollecting(lawdCd) ? State.COLLECTING : State.NONE, 0);
  }

  /** Kicks off a backfill if the region has no data and isn't already collecting. */
  public Status trigger(String lawdCd) {
    Status current = status(lawdCd);
    if (current.state() == State.NONE) {
      worker.backfill(lawdCd, BACKFILL_MONTHS);
      return new Status(State.COLLECTING, 0);
    }
    return current;
  }
}
