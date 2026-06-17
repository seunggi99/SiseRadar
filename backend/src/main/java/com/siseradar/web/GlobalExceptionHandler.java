package com.siseradar.web;

import com.siseradar.collect.DataGoKrQuotaException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 회복 가능한 예외를 깔끔히 매핑한다. 특히 data.go.kr 일일 쿼터 초과(429)는 수집 중 흔히
 * 발생하는데, 기본 동작은 80줄짜리 스택트레이스를 ERROR로 남겨 로그를 폭주시킨다(Railway
 * 로그 rate-limit 유발). 여기서 한 줄 WARN + 429로 처리해 스택트레이스 홍수를 막는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(DataGoKrQuotaException.class)
  public ResponseEntity<Map<String, Object>> handleQuota(DataGoKrQuotaException e) {
    log.warn("{}", e.getMessage());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(Map.of("status", 429, "error", "data.go.kr quota exceeded", "detail", e.getMessage()));
  }
}
