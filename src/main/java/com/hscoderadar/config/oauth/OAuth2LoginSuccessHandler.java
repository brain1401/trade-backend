package com.hscoderadar.config.oauth;

import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.domain.user.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v6.1 OAuth2 로그인 성공 후 JWT 토큰 발급 처리 핸들러
 *
 * <p>v6.1 주요 변경사항: - Spring Session 제거 → JWT 토큰 발급으로 전환 - Access Token (30분) + Refresh Token
 * (1일/30일) 정책 적용 - HttpOnly 쿠키로 Refresh Token 저장 - 프론트엔드 리디렉션에 Access Token 포함
 *
 * <p>처리 과정: 1. OAuth2 인증 정보에서 사용자 정보 추출 2. JWT 토큰 발급 (Access + Refresh) 3. Refresh Token을 HttpOnly
 * 쿠키에 저장 4. Access Token과 함께 프론트엔드로 리디렉트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;

  @Value("${oauth2.frontend.callback-url:http://localhost:3000/auth/callback}")
  private String frontendCallbackUrl;

  /** OAuth2 로그인 성공 시 JWT 토큰 발급 후 리다이렉트 (v6.1) */
  @Override
  @Transactional
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    log.info("OAuth2 로그인 성공 처리 시작 (v6.1 JWT 방식)");

    try {
      // 1. OAuth2 사용자 정보 추출
      PrincipalDetails principalDetails = extractPrincipalDetails(authentication);
      User user = principalDetails.getUser();

      log.debug("OAuth2 사용자 인증 완료: email={}", user.getEmail());

      // 2. 세션에서 rememberMe 값 확인 (AuthController에서 설정)
      Boolean rememberMe = (Boolean) request.getSession().getAttribute("rememberMe");
      if (rememberMe == null) {
        rememberMe = true; // OAuth는 기본값 true
      }

      log.debug("OAuth rememberMe 설정: {}", rememberMe);

      // 3. v6.1 JWT 세부화: rememberMe 옵션을 고려한 토큰 생성
      JwtTokenProvider.TokenInfo tokenInfo =
          jwtTokenProvider.generateToken(authentication, rememberMe);

      // 4. v6.1 설계: Refresh Token만 HttpOnly 쿠키에 저장
      setRefreshTokenCookie(response, tokenInfo.refreshToken(), rememberMe);

      // 5. v6.1 설계: Access Token은 URL 파라미터로 프론트엔드에 전달 (Zustand 저장용)
      String redirectUrl = buildSuccessRedirectUrl(tokenInfo.accessToken(), user);
      log.info("OAuth2 로그인 성공: email={}, redirectUrl={}", user.getEmail(), redirectUrl);

      // 6. 세션에서 rememberMe 제거
      request.getSession().removeAttribute("rememberMe");

      response.sendRedirect(redirectUrl);

    } catch (Exception e) {
      log.error("OAuth2 로그인 성공 처리 중 오류 발생", e);
      handleOAuthError(response, e);
    }
  }

  /** Authentication 객체에서 PrincipalDetails 추출 */
  private PrincipalDetails extractPrincipalDetails(Authentication authentication) {
    Object principal = authentication.getPrincipal();

    if (!(principal instanceof PrincipalDetails)) {
      log.error(
          "OAuth2 인증 객체 타입 오류: expected=PrincipalDetails, actual={}",
          principal.getClass().getSimpleName());
      throw AuthException.invalidToken();
    }

    return (PrincipalDetails) principal;
  }

  /** Refresh Token을 HttpOnly 쿠키에 설정 (v6.1 보안 정책) */
  private void setRefreshTokenCookie(
      HttpServletResponse response, String refreshToken, boolean rememberMe) {
    Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
    refreshCookie.setHttpOnly(true); // XSS 방지
    refreshCookie.setSecure(true); // v6.1: 운영 환경과 동일하게 true로 변경
    refreshCookie.setPath("/api/auth/refresh"); // v6.1: AuthController와 경로 통일

    // v6.1 JWT 세부화 정책에 따른 만료 시간 설정
    if (rememberMe) {
      refreshCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
      log.debug("Refresh Token HttpOnly 쿠키 설정: 30일 (remember me)");
    } else {
      refreshCookie.setMaxAge(24 * 60 * 60); // 1일
      log.debug("Refresh Token HttpOnly 쿠키 설정: 1일 (일반)");
    }

    refreshCookie.setAttribute("SameSite", "Strict"); // v6.1: CSRF 방지 강화 (Strict)

    response.addCookie(refreshCookie);
  }

  /** 성공 리다이렉트 URL 구성 (v6.1 JWT 토큰 포함) */
  private String buildSuccessRedirectUrl(String accessToken, User user) {
    try {
      String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
      String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
      String encodedName = URLEncoder.encode(user.getName(), StandardCharsets.UTF_8);

      return String.format(
          "%s?status=success&accessToken=%s&email=%s&name=%s",
          frontendCallbackUrl, encodedToken, encodedEmail, encodedName);
    } catch (Exception e) {
      log.error("리디렉트 URL 인코딩 실패", e);
      return frontendCallbackUrl + "?status=error&message=encoding_error";
    }
  }

  /** OAuth 오류 처리 */
  private void handleOAuthError(HttpServletResponse response, Exception error) throws IOException {
    log.error("OAuth2 로그인 처리 중 오류 발생", error);

    // 에러 페이지로 리다이렉트
    String errorUrl = frontendCallbackUrl + "?status=error&message=oauth_error";
    response.sendRedirect(errorUrl);
  }
}
