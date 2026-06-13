package com.siseradar.web;

import com.siseradar.collect.KakaoClient;
import com.siseradar.collect.KakaoClient.ResolvedRegion;
import com.siseradar.collect.RegionCollectionService;
import com.siseradar.collect.RegionCollectionService.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
@Tag(name = "Regions", description = "지역 데이터 수집 상태 / 좌표 변환")
public class RegionController {

  private final RegionCollectionService regionCollection;
  private final KakaoClient kakaoClient;

  public RegionController(RegionCollectionService regionCollection, KakaoClient kakaoClient) {
    this.regionCollection = regionCollection;
    this.kakaoClient = kakaoClient;
  }

  @GetMapping("/resolve")
  @Operation(summary = "좌표(x=lng, y=lat) → 법정동 5자리 시군구 코드 (카카오)")
  public ResolvedRegion resolve(@RequestParam double x, @RequestParam double y) {
    return kakaoClient.coord2region(x, y);
  }

  @GetMapping("/{lawdCd}/status")
  @Operation(summary = "지역의 수집 상태 (NONE/COLLECTING/DONE + 수집된 개월 수)")
  public Status status(@PathVariable String lawdCd) {
    return regionCollection.status(lawdCd);
  }

  @PostMapping("/{lawdCd}/collect")
  @Operation(summary = "데이터 없는 지역의 최근 24개월 백필을 백그라운드로 시작")
  public Status collect(@PathVariable String lawdCd) {
    return regionCollection.trigger(lawdCd);
  }
}
