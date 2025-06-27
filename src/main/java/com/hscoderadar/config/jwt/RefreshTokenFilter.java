package com.hscoderadar.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refresh Token 전용 필터
 *
 * <p>v6.1 정책: /api/auth/refresh 엔드포인트에서만 Refresh Token 쿠키를 처리 - 일반 인증 필터와 분리하여 토큰 역할을 명확히 구분 -
 * HttpOnly 쿠키에서 Refresh Token을 추출하여 인증 처리
 */
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();

    String servletPath = path;
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      servletPath = path.substring(contextPath.length());
    }

    // /api/auth/refresh 경로에서만 필터 실행
    return !servletPath.equals("/api/auth/refresh");
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // HttpOnly 쿠키에서 Refresh Token 추출
    String refreshToken = extractRefreshTokenFromCookie(request);

    if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
      try {
        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Refresh Token 인증 성공: {}", authentication.getName());
      } catch (Exception e) {
        log.warn("Refresh Token 인증 실패: {}", e.getMessage());
        SecurityContextHolder.clearContext();
      }
    }

    filterChain.doFilter(request, response);
  }

  private String extractRefreshTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("refreshToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
          log.debug("HttpOnly 쿠키에서 Refresh Token 추출");
          return cookie.getValue().trim();
        }
      }
    }
    return null;
  }
}
