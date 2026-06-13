package com.siseradar.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Reads the authenticated user id (set by {@link JwtAuthenticationFilter}) from the context. */
public final class CurrentUser {

  private CurrentUser() {}

  public static Long id() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
    }
    return userId;
  }
}
