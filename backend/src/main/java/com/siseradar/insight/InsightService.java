package com.siseradar.insight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseradar.collect.KoreaRegions;
import com.siseradar.domain.PropertyType;
import com.siseradar.domain.RegionInsight;
import com.siseradar.domain.TradeType;
import com.siseradar.insight.InsightBasis.BandCount;
import com.siseradar.repository.RegionInsightRepository;
import com.siseradar.web.StatsService;
import com.siseradar.web.dto.MonthlyStatsResponse;
import com.siseradar.web.dto.SameStoreChangeResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Produces a grounded, natural-language 시장 요약 for a region. All numbers come from the same
 * {@link StatsService} the dashboard uses (never invented). Summaries are cached per
 * (region, type, period); the LLM is called only on a cache miss, a basis change, or TTL expiry.
 */
@Service
public class InsightService {

  private static final double PYEONG = 3.3058; // ㎡ → 평
  private static final Duration TTL = Duration.ofHours(24);

  private final StatsService stats;
  private final GeminiClient gemini;
  private final RegionInsightRepository repo;
  private final KoreaRegions regions;
  private final ObjectMapper json;

  public InsightService(
      StatsService stats,
      GeminiClient gemini,
      RegionInsightRepository repo,
      KoreaRegions regions,
      ObjectMapper json) {
    this.stats = stats;
    this.gemini = gemini;
    this.repo = repo;
    this.regions = regions;
    this.json = json;
  }

  public RegionInsightResponse regionInsight(
      String lawdCd, PropertyType pt, TradeType tt, String from, String to) {
    InsightBasis basis = buildBasis(lawdCd, pt, tt, from, to);

    // 데이터 없음 → LLM 호출/캐시 없이 명확한 문장 반환
    if (!basis.hasData()) {
      return new RegionInsightResponse(
          regionLabel(lawdCd)
              + " "
              + basis.propertyLabel()
              + " "
              + basis.tradeLabel()
              + "는 표시할 거래 데이터가 부족합니다.",
          Instant.now(),
          "fallback",
          basis);
    }

    String period = basis.periodFrom() + "-" + basis.periodTo();
    String basisJson = toJson(basis);
    Optional<RegionInsight> cached =
        repo.findByLawdCdAndPropertyTypeAndTradeTypeAndPeriod(
            lawdCd, pt.name(), tt.name(), period);

    // 캐시 히트: 같은 수치 + TTL 이내 + 직전이 AI 생성이면 그대로 (매 요청 LLM 호출 금지)
    if (cached.isPresent()) {
      RegionInsight ri = cached.get();
      boolean fresh =
          ri.getBasisJson().equals(basisJson)
              && ri.getGeneratedAt().isAfter(Instant.now().minus(TTL))
              && "ai".equals(ri.getSource());
      if (fresh) {
        return new RegionInsightResponse(ri.getSummary(), ri.getGeneratedAt(), ri.getSource(), basis);
      }
    }

    // (재)생성: LLM 우선, 실패/쿼터 소진 시 템플릿 폴백
    String generated = gemini.generate(buildPrompt(basis));
    String summary = generated != null ? generated : templateSummary(basis);
    String source = generated != null ? "ai" : "fallback";

    RegionInsight ri =
        cached.orElseGet(
            () ->
                new RegionInsight(
                    lawdCd, pt.name(), tt.name(), period, summary, basisJson, source));
    if (cached.isPresent()) {
      ri.refresh(summary, basisJson, source);
    }
    repo.save(ri);
    return new RegionInsightResponse(ri.getSummary(), ri.getGeneratedAt(), ri.getSource(), basis);
  }

  // ── basis (그라운딩 수치) ────────────────────────────────────────────────────
  private InsightBasis buildBasis(
      String lawdCd, PropertyType pt, TradeType tt, String from, String to) {
    String propertyLabel = propertyLabel(pt);
    String tradeLabel = tt == TradeType.SALE ? "매매" : "전월세";
    String metricLabel = tt == TradeType.SALE ? "평당가(전용)" : "보증금 평당(전용)";

    List<MonthlyStatsResponse> monthly = stats.monthly(lawdCd, pt, tt, from, to);
    if (monthly.isEmpty()) {
      return new InsightBasis(
          regionLabel(lawdCd), propertyLabel, tradeLabel, metricLabel,
          null, null, 0, 0, 0, 0, 0, null, null, 0, List.of(), false);
    }

    MonthlyStatsResponse latest = monthly.get(monthly.size() - 1);
    long totalVolume = monthly.stream().mapToLong(MonthlyStatsResponse::count).sum();
    long avgPerSqm = latest.avgPricePerArea();
    long avgPerPyeong = Math.round(avgPerSqm * PYEONG);
    long medianPerPyeong = Math.round(latest.medianPricePerArea() * PYEONG);

    // 동일단지 변동률 — 지도·대시보드 카드와 동일한 단일 계산(최근 12개월 vs 직전 12개월 고정).
    // 24개월 미충족 등으로 매칭 단지 없으면 데이터 부족(null).
    SameStoreChangeResponse change = stats.sameStoreChange12(lawdCd, pt, tt);
    Double changeAvg = change.avgPct();
    Double changeMedian = change.medianPct();
    int changeMatched = (int) change.matched();

    // 평형대 분포: 기간 전체 거래량 합산
    Map<String, Long> bandTotals = new LinkedHashMap<>();
    for (MonthlyStatsResponse m : monthly) {
      for (MonthlyStatsResponse.BandStat b : m.bands()) {
        bandTotals.merge(b.band(), b.count(), Long::sum);
      }
    }
    List<BandCount> bands =
        bandTotals.entrySet().stream()
            .map(e -> new BandCount(bandLabel(e.getKey()), e.getValue()))
            .collect(Collectors.toList());

    return new InsightBasis(
        regionLabel(lawdCd),
        propertyLabel,
        tradeLabel,
        metricLabel,
        monthly.get(0).ym(),
        latest.ym(),
        monthly.size(),
        avgPerPyeong,
        medianPerPyeong,
        avgPerSqm,
        totalVolume,
        changeAvg,
        changeMedian,
        changeMatched,
        bands,
        true);
  }

  // ── prompt / fallback ───────────────────────────────────────────────────────
  private String buildPrompt(InsightBasis b) {
    StringBuilder sb = new StringBuilder();
    sb.append("당신은 한국 부동산 데이터 요약 도우미입니다. 아래 '확정 수치'만 사용해 한국어로 ")
        .append("2~4문장의 중립적 시장 요약을 작성하세요.\n\n")
        .append("[규칙]\n")
        .append("- 주어진 수치 외의 숫자를 절대 만들지 마세요. 가격 예측·추정 금지.\n")
        .append("- 투자 권유나 매수/매도 조언을 하지 마세요.\n")
        .append("- 평균은 거래 구성에 영향받을 수 있다는 점, 면적은 전용면적 기준이라는 점, ")
        .append("데이터 기간이 ")
        .append(b.months())
        .append("개월이라는 점을 자연스럽게 한 번 녹이세요.\n")
        .append("- 변동률은 아래 '동일단지 변동률' 값을 그대로 쓰고, '최근 12개월 대비 직전 12개월' ")
        .append("이라는 고정 비교 기간을 함께 적으세요. 데이터 기간 전체로 오해하지 마세요.\n")
        .append("- 마크다운·불릿 없이 줄글로.\n\n")
        .append("[확정 수치]\n")
        .append("지역: ").append(b.region()).append("\n")
        .append("유형: ").append(b.propertyLabel()).append(" ").append(b.tradeLabel()).append("\n")
        .append("데이터 기간: ").append(b.periodFrom()).append("~").append(b.periodTo())
        .append(" (").append(b.months()).append("개월)\n")
        .append("최근월 ").append(b.metricLabel()).append(" 평균: ").append(b.avgPerPyeong())
        .append("만원/평, 중위: ").append(b.medianPerPyeong()).append("만원/평\n")
        .append("기간 총 거래량: ").append(b.totalVolume()).append("건\n")
        .append("동일단지(같은 건물) 변동률 [최근 12개월 대비 직전 12개월] — 평균: ")
        .append(changeText(b.changeAvgPct()))
        .append(", 중위: ").append(changeText(b.changeMedianPct()))
        .append(b.changeMatched() > 0 ? " (동일 " + b.changeMatched() + "단지)" : "")
        .append("\n")
        .append("평형대 분포(거래량): ").append(bandsText(b.bands())).append("\n");
    return sb.toString();
  }

  private String templateSummary(InsightBasis b) {
    return String.format(
        "%s %s %s는 최근월(%s) 기준 %s 평균 %,d만원/평(중위 %,d만원/평)이며, "
            + "%s~%s(%d개월) 총 %,d건이 거래됐습니다. 같은 건물 기준 동일단지 변동률(최근 12개월 대비 "
            + "직전 12개월)은 평균 %s(중위 %s)입니다. 평균은 거래 구성에 영향받을 수 있으며 면적은 전용면적 기준입니다.",
        b.region(), b.propertyLabel(), b.tradeLabel(), b.periodTo(), b.metricLabel(),
        b.avgPerPyeong(), b.medianPerPyeong(), b.periodFrom(), b.periodTo(), b.months(),
        b.totalVolume(), changeText(b.changeAvgPct()), changeText(b.changeMedianPct()));
  }

  private static String changeText(Double pct) {
    if (pct == null) {
      return "데이터 부족";
    }
    return (pct > 0 ? "+" : "") + pct + "%";
  }

  private static String bandsText(List<BandCount> bands) {
    if (bands.isEmpty()) {
      return "데이터 부족";
    }
    return bands.stream()
        .map(b -> b.band() + " " + b.count() + "건")
        .collect(Collectors.joining(", "));
  }

  private String regionLabel(String lawdCd) {
    String name = regions.name(lawdCd);
    return name != null ? name : lawdCd;
  }

  private static String propertyLabel(PropertyType pt) {
    return switch (pt) {
      case APT -> "아파트";
      case OFFICETEL -> "오피스텔";
      case ROW_HOUSE -> "연립다세대";
      case DETACHED -> "단독다가구";
      case LAND -> "토지";
      case COMMERCIAL -> "상업업무용";
      case INDUSTRIAL -> "공장창고";
      case PRESALE_RIGHT -> "분양권";
    };
  }

  private static String bandLabel(String band) {
    return switch (band) {
      case "SMALL" -> "60㎡이하";
      case "MID_SMALL" -> "60~85㎡";
      case "MID_LARGE" -> "85~135㎡";
      case "LARGE" -> "135㎡초과";
      default -> band;
    };
  }

  private String toJson(InsightBasis basis) {
    try {
      return json.writeValueAsString(basis);
    } catch (JsonProcessingException e) {
      // basis is a plain record — serialization shouldn't fail; degrade to a stable string
      return basis.toString();
    }
  }
}
