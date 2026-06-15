package com.siseradar.map;

import com.siseradar.domain.PropertyType;
import com.siseradar.domain.TradeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map")
@Tag(name = "Map", description = "지도 시세 시각화")
public class MapController {

  private final MapService mapService;

  public MapController(MapService mapService) {
    this.mapService = mapService;
  }

  @GetMapping("/complexes")
  @Operation(
      summary = "단지 마커 (아파트·오피스텔·연립, 전용 단위면적가)",
      description = "뷰포트 bbox(swLat/swLng/neLat/neLng)가 모두 주어지면 화면 안 시군구의 단지를, 아니면 lawdCd 한 지역의 단지를 반환")
  public List<MapComplexResponse> complexes(
      @RequestParam(required = false) String lawdCd,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String band,
      @RequestParam(required = false) Double swLat,
      @RequestParam(required = false) Double swLng,
      @RequestParam(required = false) Double neLat,
      @RequestParam(required = false) Double neLng) {
    if (swLat != null && swLng != null && neLat != null && neLng != null) {
      return mapService.complexesInBounds(
          swLat, swLng, neLat, neLng, propertyType, tradeType, from, to, band);
    }
    if (lawdCd == null || lawdCd.isBlank()) {
      return List.of();
    }
    return mapService.complexes(lawdCd, propertyType, tradeType, from, to, band);
  }

  @GetMapping("/complex-change")
  @Operation(
      summary = "단일 단지 평당가(전용) 변동률 (현재 12개월 vs 직전 12개월, 같은 건물·평형대)",
      description = "두 기간 모두 거래가 있어야 계산 — 한쪽이라도 없으면 hasData=false(변동 데이터 부족)")
  public MapComplexChangeResponse complexChange(
      @RequestParam String lawdCd,
      @RequestParam String buildingName,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String band) {
    return mapService.complexChange(lawdCd, propertyType, tradeType, buildingName, band);
  }

  @GetMapping("/regions")
  @Operation(summary = "저줌 지역 버블 (전체 거래 기준 전용 단위면적가·거래량, 카카오 호출 0)")
  public List<MapRegionResponse> regions(
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String band) {
    return mapService.regions(propertyType, tradeType, from, to, band);
  }
}
