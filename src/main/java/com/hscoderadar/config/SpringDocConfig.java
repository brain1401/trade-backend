package com.hscoderadar.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc (Swagger) 설정 클래스
 *
 * <p>
 * API 문서의 기본 정보, 서버 URL, 보안 관련 설정을 정의.
 *
 * @author HsCodeRadar Team
 * @since 6.1.0
 */
@Configuration
public class SpringDocConfig {

  /**
   * OpenAPI Bean을 생성하여 API 문서를 설정.
   *
   * @return 설정이 완료된 OpenAPI 객체
   */
  @Bean
  public OpenAPI openAPI() {
    Info info = new Info()
        .title("AI 기반 무역 규제 레이더 플랫폼 API")
        .description(
            "HS Code 분석 및 무역 규제 모니터링을 위한 백엔드 서비스 API 명세서. JWT 토큰 인증이 필요합니다.")
        .version("v6.1");

    // JWT 인증 스키마 설정
    SecurityScheme securityScheme = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .in(SecurityScheme.In.HEADER)
        .name("Authorization");

    // 보안 요구사항 설정
    SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

    return new OpenAPI()
        .info(info)
        .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
        .security(List.of(securityRequirement));
  }
}