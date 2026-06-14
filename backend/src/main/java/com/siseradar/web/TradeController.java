package com.siseradar.web;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import com.siseradar.repository.RealEstateTransactionRepository;
import com.siseradar.web.dto.PageResponse;
import com.siseradar.web.dto.TradeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@Tag(name = "Trades", description = "실거래 조회")
public class TradeController {

  private static final int MAX_SIZE = 200;

  private final RealEstateTransactionRepository repository;

  public TradeController(RealEstateTransactionRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  @Operation(summary = "지역/유형/기간/면적 필터로 실거래를 페이지 조회한다")
  public PageResponse<TradeResponse> trades(
      @RequestParam String lawdCd,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String aptName,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) BigDecimal areaMin,
      @RequestParam(required = false) BigDecimal areaMax,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
    Page<TradeResponse> result =
        repository
            .search(
                lawdCd, propertyType, tradeType, aptName, from, to, areaMin, areaMax,
                PageRequest.of(Math.max(page, 0), safeSize))
            .map(TradeResponse::from);
    return PageResponse.of(result);
  }
}
