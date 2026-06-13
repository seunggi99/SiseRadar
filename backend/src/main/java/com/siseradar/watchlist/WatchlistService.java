package com.siseradar.watchlist;

import com.siseradar.domain.WatchType;
import com.siseradar.domain.Watchlist;
import com.siseradar.repository.WatchlistRepository;
import com.siseradar.watchlist.WatchlistDtos.WatchlistRequest;
import com.siseradar.watchlist.WatchlistDtos.WatchlistResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WatchlistService {

  private final WatchlistRepository repository;

  public WatchlistService(WatchlistRepository repository) {
    this.repository = repository;
  }

  public List<WatchlistResponse> list(Long userId) {
    return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(WatchlistResponse::from)
        .toList();
  }

  public WatchlistResponse add(Long userId, WatchlistRequest req) {
    String lawdCd = req.lawdCd().trim();
    String aptName = req.type() == WatchType.COMPLEX ? normalizeComplexName(req) : null;

    if (repository.existsByUserIdAndTypeAndLawdCdAndAptName(userId, req.type(), lawdCd, aptName)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 관심목록에 있어요");
    }
    Watchlist saved =
        repository.save(new Watchlist(userId, req.type(), lawdCd, aptName, Instant.now()));
    return WatchlistResponse.from(saved);
  }

  public void remove(Long userId, Long id) {
    Watchlist item =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "없는 항목입니다"));
    // per-user isolation: don't reveal others' items, just 404
    if (!item.getUserId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "없는 항목입니다");
    }
    repository.delete(item);
  }

  private static String normalizeComplexName(WatchlistRequest req) {
    if (req.aptName() == null || req.aptName().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "단지 관심 등록에는 aptName이 필요합니다");
    }
    return req.aptName().trim();
  }
}
