package com.siseradar.config;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * railway 프로파일에서만 동작. 데이터소스가 만들어지기 전(환경 준비 단계)에 datasource URL이
 * JDBC 형식(jdbc:postgresql://)인지 검증해, Railway의 원형 {@code postgresql://...}를 그대로
 * 넣은 흔한 실수를 모호한 "Unable to determine Dialect" 대신 명확한 에러로 즉시 막는다.
 *
 * <p>{@code @Component}로는 JPA 초기화가 먼저 실패해 가드가 늦으므로 EnvironmentPostProcessor로 둔다.
 * 등록: {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 */
public class RailwayDatasourceGuard implements EnvironmentPostProcessor, Ordered {

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE; // ConfigData(프로파일/yml 로딩) 이후에 검사
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
    if (!Arrays.asList(env.getActiveProfiles()).contains("railway")) {
      return;
    }
    String url;
    try {
      url = env.getProperty("spring.datasource.url");
    } catch (RuntimeException e) {
      url = null; // ${RAILWAY_DB_URL} 미설정 등 placeholder 미해결
    }
    if (url == null || !url.startsWith("jdbc:postgresql:")) {
      throw new IllegalStateException(
          "railway 프로파일의 spring.datasource.url 은 'jdbc:postgresql://HOST:PORT/DB' 형식이어야 합니다. "
              + "RAILWAY_DB_URL 을 확인하세요 (Railway의 postgresql:// 원형은 그대로 쓸 수 없음). 현재 스킴: "
              + scheme(url));
    }
  }

  /** 자격증명/호스트 노출 없이 스킴만 보여준다. */
  private static String scheme(String url) {
    if (url == null || url.isBlank()) {
      return "(빈 값)";
    }
    int i = url.indexOf("://");
    return i > 0 ? url.substring(0, i + 3) + "…" : url.substring(0, Math.min(20, url.length())) + "…";
  }
}
