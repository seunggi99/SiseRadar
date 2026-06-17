package com.siseradar.config;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Railway/Heroku는 DB 접속정보를 {@code DATABASE_URL=postgresql://user:pass@host:port/db} 형태로
 * 주입하는데, Spring datasource URL은 {@code jdbc:postgresql://...} 형식이어야 한다. 이 EPP가
 * postgres(ql):// 형태를 감지하면 jdbc URL + username/password로 변환해 최우선 property source로
 * 넣어준다. 이미 jdbc 형식이거나 DATABASE_URL이 없으면 아무 것도 하지 않는다(로컬 H2·railway
 * 프로파일 무영향).
 */
public class RailwayDatabaseUrlProcessor implements EnvironmentPostProcessor, Ordered {

  // postgres://user:pass@host[:port]/db[?query]
  private static final Pattern PG =
      Pattern.compile("^postgres(?:ql)?://([^:/?#]+):([^@]*)@([^:/?#]+)(?::(\\d+))?/([^?]+)(?:\\?(.*))?$");

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
    String raw = env.getProperty("DATABASE_URL");
    if (raw == null || raw.isBlank()) {
      return;
    }
    Matcher m = PG.matcher(raw.trim());
    if (!m.matches()) {
      return; // 이미 jdbc:... 이거나 형식 불일치 → 손대지 않음
    }
    String user = m.group(1);
    String pass = m.group(2);
    String host = m.group(3);
    String port = m.group(4) != null ? m.group(4) : "5432";
    String db = m.group(5);
    String query = m.group(6);
    String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + (query != null ? "?" + query : "");

    Map<String, Object> props = new HashMap<>();
    props.put("spring.datasource.url", jdbc);
    props.put("spring.datasource.username", user);
    props.put("spring.datasource.password", pass);
    env.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseUrl", props));
  }
}
