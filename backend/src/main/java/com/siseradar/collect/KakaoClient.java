package com.siseradar.collect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Resolves a coordinate to its 법정동 5-digit 시군구 code via Kakao Local (coord2regioncode). */
@Component
public class KakaoClient {

  private final RestClient restClient = RestClient.create();
  private final KakaoProperties props;
  private final KoreaRegions koreaRegions;

  public KakaoClient(KakaoProperties props, KoreaRegions koreaRegions) {
    this.props = props;
    this.koreaRegions = koreaRegions;
  }

  public record ResolvedRegion(String lawdCd, String sido, String sigungu) {}

  public ResolvedRegion coord2region(double x, double y) {
    if (props.restKey() == null || props.restKey().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Kakao REST 키가 설정되지 않았습니다");
    }
    String url =
        "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=" + x + "&y=" + y;
    Coord2RegionResponse res;
    try {
      res =
          restClient
              .get()
              .uri(URI.create(url))
              .header("Authorization", "KakaoAK " + props.restKey())
              .retrieve()
              .body(Coord2RegionResponse.class);
    } catch (RestClientException e) {
      // Kakao 오류(쿼터 초과 등)는 500이 아니라 일시 오류로
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "지오코딩 서비스 일시 오류");
    }

    if (res == null || res.documents == null || res.documents.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 좌표의 행정구역을 찾을 수 없습니다");
    }
    // prefer the 법정동(B) document; fall back to the first
    Doc doc =
        res.documents.stream()
            .filter(d -> "B".equals(d.regionType))
            .findFirst()
            .orElse(res.documents.get(0));
    String lawdCd = doc.code == null || doc.code.length() < 5 ? "" : doc.code.substring(0, 5);

    // Authoritative guard: Kakao returns sentinel codes (90000/90005/90009) for 국외·북한·바다.
    // Only accept codes that exist in the 250 남한 시군구 마스터.
    if (!koreaRegions.contains(lawdCd)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "국내(남한) 시군구만 지원해요");
    }
    return new ResolvedRegion(lawdCd, doc.region1depthName, doc.region2depthName);
  }

  public record LatLng(double lat, double lng) {}

  /** A geocoded place: coordinate + the address string Kakao returned (for 시군구 validation). */
  public record GeoPlace(double lat, double lng, String addressName) {}

  /**
   * Forward geocode a building (keyword search) → first result's coordinate + address, or null.
   * The {@code addressName} (e.g. "경기 성남시 분당구 정자동 …") lets the caller verify the 시군구
   * in a single call — no separate reverse-geocode needed.
   */
  public GeoPlace geocodePlace(String query) {
    Place p = firstPlace("https://dapi.kakao.com/v2/local/search/keyword.json?size=1&query=", query);
    if (p == null) {
      return null;
    }
    try {
      return new GeoPlace(Double.parseDouble(p.y), Double.parseDouble(p.x), p.addressName);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Forward geocode a region name via address search → its centroid, or null. Address search
   * returns a clean REGION point (used once per 시군구 for the map bubble centroid).
   */
  public LatLng geocodeAddress(String query) {
    Place p = firstPlace("https://dapi.kakao.com/v2/local/search/address.json?size=1&query=", query);
    if (p == null) {
      return null;
    }
    try {
      return new LatLng(Double.parseDouble(p.y), Double.parseDouble(p.x));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Forward geocode via ADDRESS search (법정동+지번) → coordinate + matched address, or null. The
   * fallback for buildings whose 단지명 keyword search fails; address search resolves a 지번 even for
   * demolished/renamed complexes. Returns the address string so the caller can validate the 시군구.
   */
  public GeoPlace geocodeAddressPlace(String query) {
    Place p = firstPlace("https://dapi.kakao.com/v2/local/search/address.json?size=1&query=", query);
    if (p == null) {
      return null;
    }
    try {
      return new GeoPlace(Double.parseDouble(p.y), Double.parseDouble(p.x), p.addressName);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * First document from a Kakao search endpoint, or null when the lookup genuinely has no result.
   * Transient HTTP errors (quota exceeded, network) propagate as {@link RestClientException} so the
   * caller can avoid caching a permanent failure for a temporary problem.
   */
  private Place firstPlace(String baseUrl, String query) {
    if (props.restKey() == null || props.restKey().isBlank()) {
      return null;
    }
    String url = baseUrl + URLEncoder.encode(query, StandardCharsets.UTF_8);
    KeywordResponse res =
        restClient
            .get()
            .uri(URI.create(url))
            .header("Authorization", "KakaoAK " + props.restKey())
            .retrieve()
            .body(KeywordResponse.class); // RestClientException on HTTP error → propagate
    if (res == null || res.documents == null || res.documents.isEmpty()) {
      return null;
    }
    return res.documents.get(0);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Coord2RegionResponse {
    public List<Doc> documents;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class KeywordResponse {
    public List<Place> documents;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Place {
    @JsonProperty("x")
    public String x; // lng

    @JsonProperty("y")
    public String y; // lat

    @JsonProperty("address_name")
    public String addressName;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Doc {
    @JsonProperty("region_type")
    public String regionType;

    @JsonProperty("code")
    public String code;

    @JsonProperty("region_1depth_name")
    public String region1depthName;

    @JsonProperty("region_2depth_name")
    public String region2depthName;
  }
}
