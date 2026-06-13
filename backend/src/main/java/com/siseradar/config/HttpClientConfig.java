package com.siseradar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

  /** A default User-Agent is mandatory — data.go.kr's gateway blocks UA-less requests. */
  @Bean
  RestClient dataGoKrRestClient() {
    return RestClient.builder().defaultHeader("User-Agent", "SiseRadar/0.1").build();
  }
}
