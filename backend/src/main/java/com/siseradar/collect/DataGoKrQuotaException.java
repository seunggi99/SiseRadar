package com.siseradar.collect;

/**
 * data.go.kr 일일 API 쿼터 초과(HTTP 429). 수집 중 흔히 발생하는 회복 가능한 상황이라,
 * 80줄짜리 스택트레이스를 남기는 대신 글로벌 핸들러가 429로 깔끔히 매핑한다. 쿼터는 자정(KST)
 * 리셋되므로 호출측(수집 스크립트)은 이 응답을 보고 나중에 재시도하면 된다.
 */
public class DataGoKrQuotaException extends RuntimeException {
  public DataGoKrQuotaException(String operationPath) {
    super("data.go.kr 일일 쿼터 초과(429): " + operationPath);
  }
}
