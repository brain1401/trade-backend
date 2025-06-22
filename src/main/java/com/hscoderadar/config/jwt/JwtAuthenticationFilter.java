package com.hscoderadar.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 하이브리드 JWT 토큰 인증 필터
 * 
 * <p>
 * 이 필터는 Public API 우선 설계와 선택적 개인화를 지원하는 하이브리드 인증 시스템을 구현합니다.
 * Authorization 헤더와 HttpOnly 쿠키 모두에서 JWT 토큰을 읽을 수 있으며,
 * 토큰의 유효성을 검증한 후 Spring Security Context에 인증 정보를 설정합니다.
 * 
 * <h3>토큰 읽기 우선순위:</h3>
 * <ol>
 * <li>HttpOnly 쿠키에서 토큰 추출 (하이브리드 인증용)</li>
 * <li>Authorization 헤더에서 Bearer 토큰 추출 (기존 API 호환성)</li>
 * </ol>
 * 
 * <h3>필터 동작 순서:</h3>
 * <ol>
 * <li>HTTP 요청에서 JWT 토큰 추출 (쿠키 우선, 헤더 보조)</li>
 * <li>JWT 토큰 유효성 검증 (서명, 만료시간 등)</li>
 * <li>유효한 토큰인 경우 사용자 인증 정보 추출</li>
 * <li>SecurityContext에 Authentication 객체 설정</li>
 * <li>다음 필터 체인으로 요청 전달</li>
 * </ol>
 * 
 * <h3>보안 특징:</h3>
 * <ul>
 * <li>HttpOnly 쿠키 우선 지원으로 XSS 공격 방지</li>
 * <li>OncePerRequestFilter 상속으로 요청당 한 번만 실행 보장</li>
 * <li>토큰 없거나 유효하지 않은 경우 인증 없이 다음 필터로 진행</li>
 * <li>SecurityContext는 요청별로 독립적으로 관리</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 2.1.0
 * @see JwtTokenProvider
 * @see OncePerRequestFilter
 * @see SecurityContextHolder
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 특정 요청에 대해 JWT 필터를 건너뛸지 결정하는 메서드
     * 
     * <p>
     * 개발 환경에서 H2 콘솔 접근 시 JWT 인증을 건너뛰도록 설정합니다.
     * Context path가 설정된 경우를 고려하여 정확한 경로 매칭을 수행합니다.
     * 이를 통해 H2 콘솔에 접근할 때 UsernameNotFoundException이 발생하지 않습니다.
     * 
     * @param request HTTP 요청 객체
     * @return true면 필터 건너뛰기, false면 필터 실행
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
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
     * 모든 HTTP 요청에 대해 하이브리드 JWT 토큰 인증을 처리하는 핵심 메서드
     * 
     * <p>
     * OncePerRequestFilter의 핵심 메서드를 구현하여 요청당 한 번만 실행되도록 보장합니다.
     * HttpOnly 쿠키를 우선적으로 확인하고, 없을 경우 Authorization 헤더를 확인하여
     * 기존 API와의 호환성을 유지합니다.
     * 
     * <h3>처리 로직:</h3>
     * <ol>
     * <li>HttpOnly 쿠키에서 토큰 추출 시도</li>
     * <li>쿠키에 토큰이 없으면 Authorization 헤더에서 Bearer 토큰 추출</li>
     * <li>토큰이 존재하고 유효한 경우 인증 정보 생성</li>
     * <li>SecurityContextHolder에 인증 정보 저장</li>
     * </ol>
     * 
     * <h3>NonNull 어노테이션 사용 이유:</h3>
     * <p>
     * OncePerRequestFilter 부모 클래스에서 이 매개변수들을 null이 아니라고 명시했기 때문에
     * 오버라이드할 때도 동일하게 @NonNull을 붙여서 컴파일러 경고를 해결합니다.
     * 
     * @param request     HTTP 요청 객체 (null이 될 수 없음)
     * @param response    HTTP 응답 객체 (null이 될 수 없음)
     * @param filterChain 다음 필터로 요청을 전달하는 체인 (null이 될 수 없음)
     * @throws ServletException 서블릿 처리 중 오류 발생 시
     * @throws IOException      I/O 처리 중 오류 발생 시
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. HttpOnly 쿠키 우선, Authorization 헤더 보조로 JWT 토큰 추출
        String token = resolveToken(request);

        // 2. validateToken으로 토큰 유효성 검사
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
     * HTTP 요청에서 JWT 토큰을 하이브리드 방식으로 안전하게 추출하는 메서드
     * 
     * <p>
     * 하이브리드 인증 시스템을 지원하기 위해 다음 순서로 토큰을 찾습니다:
     * <ol>
     * <li>HttpOnly 쿠키 "token"에서 추출 (보안 우선)</li>
     * <li>Authorization 헤더의 "Bearer " 토큰 추출 (호환성 지원)</li>
     * </ol>
     * 
     * <h3>지원하는 형식:</h3>
     * 
     * <pre>
     * Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (우선)
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (보조)
     * </pre>
     * 
     * <h3>검증 로직:</h3>
     * <ul>
     * <li>HttpOnly 쿠키에서 "token" 쿠키 확인</li>
     * <li>쿠키가 없으면 Authorization 헤더 확인</li>
     * <li>"Bearer " 접두사로 시작하는지 확인</li>
     * <li>접두사 제거 후 토큰 문자열 추출</li>
     * <li>공백 문자 제거 (trim)</li>
     * </ul>
     * 
     * @param request HTTP 요청 객체
     * @return 추출된 JWT 토큰 문자열, 없거나 형식이 잘못된 경우 null
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. HttpOnly 쿠키에서 토큰 추출 시도 (보안 우선)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    log.debug("HttpOnly 쿠키에서 JWT 토큰 추출");
                    return cookie.getValue().trim();
                }
            }
        }

        // 2. Authorization 헤더에서 Bearer 토큰 추출 (호환성 지원)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            log.debug("Authorization 헤더에서 JWT 토큰 추출");
            return bearerToken.substring(7).trim();
        }

        return null;
    }
}