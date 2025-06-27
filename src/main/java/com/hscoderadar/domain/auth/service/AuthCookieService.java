package com.hscoderadar.domain.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 인증 관련 쿠키 처리를 전담하는 서비스 (v6.1)
 *
 * <p>HttpOnly, Secure, SameSite=Strict 속성이 적용된 Refresh Token 쿠키의 생성, 조회, 삭제를 중앙에서 관리하여 컨트롤러의 부담을
 * 줄이고 쿠키 정책의 일관성을 보장함.
 *
 * @author HsCodeRadar Team
 * @since 6.1.0
 */
@Service
public class AuthCookieService {

  private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
  private static final String COOKIE_PATH = "/api/auth/refresh";
  private static final int ONE_DAY_IN_SECONDS = 24 * 60 * 60;
  private static final int THIRTY_DAYS_IN_SECONDS = 30 * 24 * 60 * 60;

  /**
   * Refresh Token을 담은 HttpOnly 쿠키를 생성함.
   *
   * @param refreshToken 쿠키에 담을 Refresh Token 값
   * @param rememberMe '로그인 기억하기' 여부 (쿠키 만료 시간 결정)
   * @return 생성된 {@link Cookie} 객체
   */
  public Cookie createRefreshTokenCookie(String refreshToken, boolean rememberMe) {
    Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath(COOKIE_PATH);
    cookie.setAttribute("SameSite", "Strict");
    cookie.setMaxAge(rememberMe ? THIRTY_DAYS_IN_SECONDS : ONE_DAY_IN_SECONDS);
    return cookie;
  }

  /**
   * HTTP 요청에서 Refresh Token 값을 추출함.
   *
   * @param request HTTP 요청 객체
   * @return 추출된 Refresh Token. 없으면 null.
   */
  public String getRefreshTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    return Arrays.stream(request.getCookies())
        .filter(
            cookie ->
                REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())
                    && StringUtils.hasText(cookie.getValue()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }

  /**
   * 모든 Refresh Token 관련 쿠키를 삭제하는 쿠키 배열을 생성함.
   *
   * <p>기존에 잘못된 경로로 설정되었을 수 있는 쿠키까지 모두 정리하기 위해 여러 경로의 삭제 쿠키를 생성하여 반환함.
   *
   * @return 삭제를 위한 쿠키 배열
   */
  public Cookie[] clearRefreshTokenCookies() {
    Cookie mainCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
    mainCookie.setHttpOnly(true);
    mainCookie.setSecure(true);
    mainCookie.setPath(COOKIE_PATH);
    mainCookie.setMaxAge(0);
    mainCookie.setAttribute("SameSite", "Strict");

    // 호환성을 위해 기존에 잘못 설정된 Path=/ 쿠키도 정리
    Cookie misconfiguredCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
    misconfiguredCookie.setHttpOnly(true);
    misconfiguredCookie.setSecure(true);
    misconfiguredCookie.setPath("/");
    misconfiguredCookie.setMaxAge(0);
    misconfiguredCookie.setAttribute("SameSite", "Strict");

    // 레거시 refresh_token 쿠키 정리
    Cookie legacyRefreshCookie = new Cookie("refresh_token", "");
    legacyRefreshCookie.setHttpOnly(true);
    legacyRefreshCookie.setSecure(true);
    legacyRefreshCookie.setPath("/");
    legacyRefreshCookie.setMaxAge(0);
    legacyRefreshCookie.setAttribute("SameSite", "Strict");

    return new Cookie[] {mainCookie, misconfiguredCookie, legacyRefreshCookie};
  }
}
