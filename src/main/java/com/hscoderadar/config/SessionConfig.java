package com.hscoderadar.config;

import org.springframework.context.annotation.Configuration;

/**
 * v6.1 JWT 전용 설정 (Spring Session 제거)
 *
 * <p>v6.1 요구사항 변경: - 완전한 Stateless JWT 인증 시스템 - Spring Session 제거 (SessionConfig 비활성화) - JWT Access
 * Token (30분) + Refresh Token (1일/30일) 정책 - HttpOnly 쿠키는 Refresh Token 저장용으로만 사용
 *
 * <p>주요 특징: - 세션리스(Stateless) 아키텍처 - Redis는 JWT 블랙리스트 및 임시 토큰 상태 관리용으로만 사용 - OAuth2 성공 후 JWT 토큰
 * 발급으로 통일
 */
@Configuration
public class SessionConfig {

  // v6.1: Spring Session 비활성화
  // @EnableRedisHttpSession 제거
  // 완전한 JWT 기반 인증으로 전환

  // HttpOnly 쿠키는 JwtAuthenticationFilter에서 직접 처리
  // CookieSerializer 설정 불필요 (JWT 토큰 자체가 보안 토큰)
}
