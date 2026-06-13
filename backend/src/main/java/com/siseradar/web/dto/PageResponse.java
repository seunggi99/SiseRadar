package com.siseradar.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable, explicit page envelope (avoids serializing Spring's Page directly). */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
