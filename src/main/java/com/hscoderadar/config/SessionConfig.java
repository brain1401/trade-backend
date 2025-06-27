package com.hscoderadar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session 설정 클래스 (v4.2)
 * 
 * <p>
 * v4.2에서 도입된 Spring Session 기반 세션 관리를 위한 설정
 * 복잡한 JWT 토큰 관리를 제거하고 표준 HTTP 세션을 Redis에 저장
 * 
 * <h3>주요 특징:</h3>
 * <ul>
 * <li>Redis 기반 세션 저장소 사용</li>
 * <li>HttpOnly, Secure 쿠키 정책 적용</li>
 * <li>세션 고정 공격 방지</li>
 * <li>30분 세션 유효시간</li>
 * </ul>
 * 
 * <h3>보안 강화:</h3>
 * <ul>
 * <li>HttpOnly: JavaScript를 통한 쿠키 접근 차단</li>
 * <li>Secure: HTTPS에서만 쿠키 전송 (운영환경)</li>
 * <li>SameSite: CSRF 공격 방지</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 4.2.0
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30분
public class SessionConfig {

  /**
   * 세션 쿠키 직렬화 설정
   * 
   * <p>
   * Spring Session에서 사용하는 세션 쿠키의 보안 정책을 설정
   * 
   * @return 보안이 강화된 쿠키 직렬화 설정
   */
  @Bean
  public CookieSerializer cookieSerializer() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();

    // 쿠키 이름 설정
    serializer.setCookieName("HSCODE_SESSION");

    // 쿠키 경로 설정 (루트 경로)
    serializer.setCookiePath("/");

    // HttpOnly 설정 (JavaScript 접근 차단)
    serializer.setUseHttpOnlyCookie(true);

    // Secure 설정 (HTTPS에서만 전송 - 운영환경에서 활성화)
    // 개발환경에서는 false로 설정 (HTTP 허용)
    serializer.setUseSecureCookie(false); // TODO: 운영환경에서는 true로 변경

    // SameSite 설정 (CSRF 공격 방지)
    serializer.setSameSite("Lax");

    // 도메인 설정 (하위 도메인 포함)
    // serializer.setDomainName(".hscoderadar.com"); // TODO: 운영환경에서 활성화

    return serializer;
  }
}