package com.siseradar.config;

import com.siseradar.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtFilter;
  private final CorsConfigurationSource corsConfigurationSource;

  public SecurityConfig(
      JwtAuthenticationFilter jwtFilter, CorsConfigurationSource corsConfigurationSource) {
    this.jwtFilter = jwtFilter;
    this.corsConfigurationSource = corsConfigurationSource;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // public read + auth + docs
                    .requestMatchers("/api/auth/**", "/api/health", "/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/trades/**",
                        "/api/stats/**",
                        "/api/map/**",
                        "/api/insights/**",
                        "/api/diag/**")
                    .permitAll()
                    // region status + on-demand collect are public (dashboard has no login)
                    .requestMatchers("/api/regions/**")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/h2-console/**")
                    .permitAll()
                    // everything else (watchlist, alerts, notifications, internal) requires a token
                    .anyRequest()
                    .authenticated())
        // allow the H2 console to render in a frame (dev only)
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
