package com.siseradar.repository;

import com.siseradar.domain.RegionInsight;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionInsightRepository extends JpaRepository<RegionInsight, Long> {

  Optional<RegionInsight> findByLawdCdAndPropertyTypeAndTradeTypeAndPeriod(
      String lawdCd, String propertyType, String tradeType, String period);
}
