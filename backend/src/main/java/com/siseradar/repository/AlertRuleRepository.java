package com.siseradar.repository;

import com.siseradar.domain.AlertRule;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

  List<AlertRule> findByUserIdOrderByCreatedAtDesc(Long userId);

  /** Rules attached to any of the given watchlist items — used during alert evaluation. */
  List<AlertRule> findByWatchlistIdIn(Collection<Long> watchlistIds);

  long countByWatchlistId(Long watchlistId);
}
