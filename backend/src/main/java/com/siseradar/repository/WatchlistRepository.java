package com.siseradar.repository;

import com.siseradar.domain.Watchlist;
import com.siseradar.domain.WatchType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

  List<Watchlist> findByUserIdOrderByCreatedAtDesc(Long userId);

  boolean existsByUserIdAndTypeAndLawdCdAndAptName(
      Long userId, WatchType type, String lawdCd, String aptName);

  /** All watchers of a region — used by alert evaluation after collection. */
  List<Watchlist> findByLawdCd(String lawdCd);
}
