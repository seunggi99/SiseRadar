package com.siseradar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

  private AuthDtos() {}

  public record SignupRequest(
      @Email @NotBlank String email, @NotBlank @Size(min = 8, max = 72) String password) {}

  public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  public record AuthResponse(String token, String email) {}
}
