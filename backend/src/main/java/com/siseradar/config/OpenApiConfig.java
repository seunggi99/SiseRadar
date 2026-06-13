package com.siseradar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI siseRadarOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("SiseRadar API")
                .version("v1")
                .description("한국 아파트 실거래가 분석 API"));
  }
}
