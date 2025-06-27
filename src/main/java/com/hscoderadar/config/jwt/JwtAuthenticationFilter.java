package com.hscoderadar.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
 * JWT Access Token 인증 필터
 *
 * <p>v6.1 변경된 JWT 토큰 정책을 적용하는 인증 필터. Authorization Bearer 헤더에서만 Access Token을 추출하여 인증을 처리. Refresh
 * Token은 별도의 RefreshTokenFilter에서 처리.
 *
 * <h3>v6.1 토큰 정책:</h3>
 *
 * <ul>
 *   <li>Access Token (30분): Authorization Bearer 헤더로 전송
 *   <li>Refresh Token (1일/30일): HttpOnly 쿠키, /api/auth/refresh에서만 처리
 * </ul>
 *
 * <h3>필터 동작 순서:</h3>
 *
 * <ol>
 *   <li>Authorization Bearer 헤더에서 Access Token 추출
 *   <li>JWT 토큰 유효성 검증 (서명, 만료시간, 블랙리스트 등)
 *   <li>유효한 토큰인 경우 사용자 인증 정보 추출
 *   <li>SecurityContext에 Authentication 객체 설정
 *   <li>다음 필터 체인으로 요청 전달
 * </ol>
 *
 * <h3>보안 특징:</h3>
 *
 * <ul>
 *   <li>Access Token과 Refresh Token 역할 분리
 *   <li>OncePerRequestFilter 상속으로 요청당 한 번만 실행 보장
 *   <li>토큰 없거나 유효하지 않은 경우 인증 없이 다음 필터로 진행
 *   <li>SecurityContext는 요청별로 독립적으로 관리
 * </ul>
 *
 * @author HsCodeRadar Team
 * @since 6.1.0
 * @see JwtTokenProvider
 * @see RefreshTokenFilter
 * @see OncePerRequestFilter
 * @see SecurityContextHolder
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtRedisService jwtRedisService;

  /**
   * 특정 요청에 대해 JWT 필터를 건너뛸지 결정하는 메서드
   *
   * <p>개발 환경에서 H2 콘솔 접근 시 JWT 인증을 건너뛰도록 설정. Context path가 설정된 경우를 고려하여 정확한 경로 매칭을 수행. 이를 통해 H2 콘솔에
   * 접근할 때 UsernameNotFoundException이 발생하지 않음.
   *
   * @param request HTTP 요청 객체
   * @return true면 필터 건너뛰기, false면 필터 실행
   */
  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();

    // Context path를 제거한 실제 서블릿 경로 확인
    String servletPath = path;
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      servletPath = path.substring(contextPath.length());
    }

    // H2 콘솔 경로는 JWT 필터 건너뛰기
    if (servletPath.startsWith("/h2-console")) {
      log.debug("H2 콘솔 접근으로 JWT 필터 건너뛰기: {} (원본 URI: {})", servletPath, path);
      return true;
    }

    return false;
  }

  /**
   * 모든 HTTP 요청에 대해 JWT 토큰 인증을 처리하는 핵심 메서드
   *
   * <p>OncePerRequestFilter의 핵심 메서드를 구현하여 요청당 한 번만 실행되도록 보장. HttpOnly 쿠키를 우선적으로 확인하고, 없을 경우
   * Authorization 헤더를 확인하여 기존 API와의 호환성을 유지.
   *
   * <h3>처리 로직:</h3>
   *
   * <ol>
   *   <li>HttpOnly 쿠키에서 토큰 추출 시도
   *   <li>쿠키에 토큰이 없으면 Authorization 헤더를 확인
   *   <li>토큰이 존재하고 유효한 경우 인증 정보 생성
   *   <li>SecurityContextHolder에 인증 정보 저장
   * </ol>
   *
   * <h3>NonNull 어노테이션 사용 이유:</h3>
   *
   * <p>OncePerRequestFilter 부모 클래스에서 이 매개변수들을 null이 아니라고 명시했기 때문에 오버라이드할 때도 동일하게 @NonNull을 붙여서 컴파일러
   * 경고를 해결.
   *
   * @param request HTTP 요청 객체 (null이 될 수 없음)
   * @param response HTTP 응답 객체 (null이 될 수 없음)
   * @param filterChain 다음 필터로 요청을 전달하는 체인 (null이 될 수 없음)
   * @throws ServletException 서블릿 처리 중 오류 발생 시
   * @throws IOException I/O 처리 중 오류 발생 시
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // 1. Authorization Bearer 헤더에서 Access Token 추출
    String token = resolveToken(request);

    // 2. validateToken으로 토큰 유효성 검사 및 블랙리스트 검증
    if (token != null && jwtTokenProvider.validateToken(token)) {
      try {
        // 토큰이 유효할 경우 토큰에서 Authentication 객체를 가지고 와서 SecurityContext에 저장
        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("JWT 토큰 인증 성공: {}", authentication.getName());
      } catch (Exception e) {
        // 토큰은 유효하지만 사용자 정보를 찾을 수 없는 경우 (예: 탈퇴한 회원)
        // 이 경우 인증 없이 진행하여 Public API는 계속 사용할 수 있도록 함
        log.warn("JWT 토큰 인증 실패 - 사용자 정보 없음: {}", e.getMessage());
        SecurityContextHolder.clearContext();
      }
    } else if (token != null) {
      // 토큰이 있지만 유효하지 않은 경우 로그 기록
      log.warn("유효하지 않은 JWT 토큰: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
    }

    // 다음 필터로 요청 전달 (인증 여부와 관계없이 항상 실행)
    filterChain.doFilter(request, response);
  }

  /**
   * HTTP 요청에서 JWT Access Token을 추출하는 메서드
   *
   * <p>변경된 v6.1 토큰 정책: - Access Token: Authorization Bearer 헤더에서만 추출 - Refresh Token: HttpOnly 쿠키로
   * 관리 (/api/auth/refresh에서만 처리)
   *
   * <h3>지원하는 형식:</h3>
   *
   * <pre>
   * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (Access Token)
   * </pre>
   *
   * <h3>검증 로직:</h3>
   *
   * <ul>
   *   <li>Authorization 헤더에서 "Bearer " 접두사 확인
   *   <li>접두사 제거 후 토큰 문자열 추출
   *   <li>공백 문자 제거 (trim)
   * </ul>
   *
   * @param request HTTP 요청 객체
   * @return 추출된 JWT Access Token 문자열, 없거나 형식이 잘못된 경우 null
   */
  private String resolveToken(HttpServletRequest request) {
    // Authorization Bearer 헤더에서만 Access Token 추출
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      log.debug("Authorization 헤더에서 Access Token 추출");
      return bearerToken.substring(7).trim();
    }

    log.debug("Authorization 헤더에서 유효한 Bearer 토큰을 찾을 수 없음");
    return null;
  }
}
