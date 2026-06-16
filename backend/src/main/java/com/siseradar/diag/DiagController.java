package com.siseradar.diag;

import com.siseradar.collect.DataGoKrProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY diagnostic — checks whether THIS server's outbound IP can call the data.go.kr RTMS API.
 * Calls getRTMSDataSvcAptTrade once (종로구, 직전 완전월, numOfRows=1) and reports the raw result so
 * we can tell a real success from an IP-whitelist (error 32) / quota (22) / key (30) / approval (20)
 * problem. Remove once the deploy-server call is confirmed.
 */
@RestController
@RequestMapping("/api/diag")
@Tag(name = "Diag", description = "임시 진단 (확인 후 제거)")
public class DiagController {

  private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");
  private static final String OP = "/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade";

  private final DataGoKrProperties props;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public DiagController(DataGoKrProperties props) {
    this.props = props;
  }

  @GetMapping("/datagokr")
  @Operation(summary = "이 서버 IP에서 data.go.kr 호출 가능 여부 진단 (임시)")
  public Map<String, Object> datagokr() {
    Map<String, Object> out = new LinkedHashMap<>();
    String dealYmd = YearMonth.now().minusMonths(1).format(YM); // 직전 완전월
    out.put("lawdCd", "11110");
    out.put("dealYmd", dealYmd);
    out.put("outboundIp", outboundIp());

    boolean keySet = props.serviceKey() != null && !props.serviceKey().isBlank();
    out.put("serviceKeyConfigured", keySet);
    if (!keySet) {
      out.put("error", "DATA_GO_KR_SERVICE_KEY 미설정");
      return out;
    }

    // encoding 키를 그대로 사용(이중 인코딩 금지) — 기존 수집 클라이언트와 동일 방식
    String url =
        props.baseUrl()
            + OP
            + "?serviceKey=" + props.serviceKey()
            + "&LAWD_CD=11110"
            + "&DEAL_YMD=" + dealYmd
            + "&pageNo=1&numOfRows=1";

    try {
      HttpRequest req =
          HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      String body = res.body() == null ? "" : res.body();

      out.put("httpStatus", res.statusCode());
      String resultCode = firstGroup(body, "<resultCode>(.*?)</resultCode>");
      String reasonCode = firstGroup(body, "<returnReasonCode>(.*?)</returnReasonCode>");
      out.put("resultCode", resultCode);
      out.put("resultMsg", firstGroup(body, "<resultMsg>(.*?)</resultMsg>"));
      out.put("errorCode", reasonCode != null ? reasonCode : errorish(resultCode));
      out.put(
          "errorMsg",
          coalesce(
              firstGroup(body, "<returnAuthMsg>(.*?)</returnAuthMsg>"),
              firstGroup(body, "<errMsg>(.*?)</errMsg>")));
      String totalCount = firstGroup(body, "<totalCount>(.*?)</totalCount>");
      out.put("totalCount", totalCount == null ? null : Integer.valueOf(totalCount.trim()));
      out.put("itemCount", countMatches(body, "<item>"));
      out.put("verdict", verdict(res.statusCode(), resultCode, reasonCode, countMatches(body, "<item>")));
      out.put("bodyHead", body.length() > 300 ? body.substring(0, 300) : body);
    } catch (Exception e) {
      out.put("httpStatus", null);
      out.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
    }
    return out;
  }

  /** This server's public outbound IP (best-effort, short timeout). */
  private String outboundIp() {
    try {
      HttpResponse<String> res =
          http.send(
              HttpRequest.newBuilder(URI.create("https://checkip.amazonaws.com"))
                  .timeout(Duration.ofSeconds(4))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      return res.body() == null ? null : res.body().trim();
    } catch (Exception e) {
      return null;
    }
  }

  private static String verdict(int status, String resultCode, String reasonCode, int items) {
    if (status == 200 && "000".equals(resultCode)) {
      return items > 0 ? "OK — 이 서버 IP에서 정상 수집 가능" : "OK(응답 정상) — 해당 월 데이터 0건";
    }
    String code = reasonCode != null ? reasonCode : resultCode;
    if ("32".equals(code)) return "에러32 — 등록되지 않은 도메인/IP (활용신청 IP 화이트리스트 문제)";
    if ("22".equals(code)) return "에러22 — 일일 트래픽 한도 초과";
    if ("20".equals(code)) return "에러20 — 활용 미승인";
    if ("30".equals(code) || "31".equals(code)) return "에러" + code + " — 서비스키/인코딩 문제";
    if (status == 502 || status == 503) return "HTTP " + status + " — data.go.kr/국토부 백엔드 일시 장애";
    return "확인 필요 (status=" + status + ", code=" + code + ")";
  }

  private static String errorish(String resultCode) {
    return resultCode == null || "000".equals(resultCode) ? null : resultCode;
  }

  private static String firstGroup(String s, String regex) {
    Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(s);
    return m.find() ? m.group(1).trim() : null;
  }

  private static int countMatches(String s, String needle) {
    int c = 0, i = 0;
    while ((i = s.indexOf(needle, i)) != -1) {
      c++;
      i += needle.length();
    }
    return c;
  }

  private static String coalesce(String a, String b) {
    return a != null ? a : b;
  }
}
