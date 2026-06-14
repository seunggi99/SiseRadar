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
  @Operation(summary = "지오코딩된 단지 마커 (아파트·오피스텔·연립, 전용 단위면적가)")
  public List<MapComplexResponse> complexes(
      @RequestParam String lawdCd,
      @RequestParam(required = false, defaultValue = "APT") PropertyType propertyType,
      @RequestParam(required = false, defaultValue = "SALE") TradeType tradeType,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String band) {
    return mapService.complexes(lawdCd, propertyType, tradeType, from, to, band);
  }
}
