package com.siseradar.watchlist;

import com.siseradar.domain.WatchType;
import com.siseradar.domain.Watchlist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class WatchlistDtos {

  private WatchlistDtos() {}

  public record WatchlistRequest(
      @NotNull WatchType type, @NotBlank String lawdCd, String aptName) {}

  public record WatchlistResponse(
      Long id, WatchType type, String lawdCd, String aptName, String label, Instant createdAt) {

    public static WatchlistResponse from(Watchlist w) {
      String label = w.getType() == WatchType.COMPLEX ? w.getAptName() : w.getLawdCd();
      return new WatchlistResponse(
          w.getId(), w.getType(), w.getLawdCd(), w.getAptName(), label, w.getCreatedAt());
    }
  }
}
