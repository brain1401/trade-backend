package com.hscoderadar.config.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.properties 파일의 JWT 관련 설정을 바인딩하는 클래스
 *
 * <p>v6.1 요구사항: - Access Token: 30분 - Refresh Token: remember me 체크시 30일, 미체크시 1일
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

  private String secretKey;
  private long accessTokenExpirationMs;

  // remember me 미체크시 Refresh Token 만료 시간 (1일)
  private long refreshTokenShortExpirationMs;

  // remember me 체크시 Refresh Token 만료 시간 (30일)
  private long refreshTokenLongExpirationMs;
}
