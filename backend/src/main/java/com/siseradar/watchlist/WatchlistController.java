package com.siseradar.watchlist;

import com.siseradar.auth.CurrentUser;
import com.siseradar.watchlist.WatchlistDtos.WatchlistRequest;
import com.siseradar.watchlist.WatchlistDtos.WatchlistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlist")
@Tag(name = "Watchlist", description = "관심 지역/단지 (로그인 필요)")
public class WatchlistController {

  private final WatchlistService service;

  public WatchlistController(WatchlistService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "내 관심목록 조회")
  public List<WatchlistResponse> list() {
    return service.list(CurrentUser.id());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "관심 지역/단지 추가")
  public WatchlistResponse add(@Valid @RequestBody WatchlistRequest request) {
    return service.add(CurrentUser.id(), request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "관심목록에서 제거")
  public void remove(@PathVariable Long id) {
    service.remove(CurrentUser.id(), id);
  }
}
