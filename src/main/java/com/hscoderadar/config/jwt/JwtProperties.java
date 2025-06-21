package com.hscoderadar.config.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.properties 파일의 JWT 관련 설정을 바인딩하는 클래스
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    private String secretKey;
    private long accessTokenExpirationMs;
    private long refreshTokenExpirationMs;

}