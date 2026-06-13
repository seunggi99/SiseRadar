package com.siseradar.auth;

import com.siseradar.auth.dto.AuthDtos.AuthResponse;
import com.siseradar.auth.dto.AuthDtos.LoginRequest;
import com.siseradar.auth.dto.AuthDtos.SignupRequest;
import com.siseradar.domain.User;
import com.siseradar.repository.UserRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public AuthResponse signup(SignupRequest req) {
    String email = req.email().trim().toLowerCase();
    if (users.existsByEmail(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다");
    }
    User user = users.save(new User(email, passwordEncoder.encode(req.password()), Instant.now()));
    return new AuthResponse(jwtService.generate(user.getId(), user.getEmail()), user.getEmail());
  }

  public AuthResponse login(LoginRequest req) {
    String email = req.email().trim().toLowerCase();
    User user =
        users
            .findByEmail(email)
            .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"));
    return new AuthResponse(jwtService.generate(user.getId(), user.getEmail()), user.getEmail());
  }
}
