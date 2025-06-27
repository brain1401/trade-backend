package com.hscoderadar.config.oauth;

import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.domain.users.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * v4.2 OAuth2 로그인 성공 후 Spring Session 기반 세션 관리를 처리하는 핸들러
 * 
 * <h3>v4.2 주요 개선사항:</h3>
 * <ul>
 * <li>Spring Session 기반 세션 관리로 전환</li>
 * <li>복잡한 JWT 토큰 관리 제거</li>
 * <li>HttpOnly 쿠키 자동 설정 (Spring Session이 처리)</li>
 * <li>단순화된 OAuth 성공 처리 로직</li>
 * </ul>
 * 
 * <h3>처리 과정:</h3>
 * <ol>
 * <li>OAuth2 인증 정보에서 사용자 정보 추출</li>
 * <li>Spring Session에 사용자 정보 저장</li>
 * <li>프론트엔드로 성공 리다이렉트</li>
 * </ol>
 * 
 * <h3>보안 특징:</h3>
 * <ul>
 * <li>Spring Session의 HttpOnly, Secure 쿠키 정책 활용</li>
 * <li>세션 고정 공격 방지</li>
 * <li>단순화된 인증 플로우로 보안 위험 최소화</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 4.2.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * 프론트엔드 콜백 URL
     * 
     * <p>
     * 기본값: {@code http://localhost:3000/auth/callback}
     * <p>
     * 배포 환경에서는 실제 프론트엔드 도메인으로 설정 필요
     */
    @Value("${oauth2.frontend.callback-url:http://localhost:3000/auth/callback}")
    private String frontendCallbackUrl;

    /**
     * OAuth2 로그인 성공 시 Spring Session 기반 세션 설정 후 리다이렉트 (v4.2)
     * 
     * @param request        HTTP 요청 객체
     * @param response       HTTP 응답 객체
     * @param authentication Spring Security 인증 객체
     * @throws IOException      I/O 처리 중 오류 발생 시
     * @throws ServletException 서블릿 처리 중 오류 발생 시
     */
    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        log.info("OAuth2 로그인 성공 처리 시작");

        try {
            // 1. OAuth2 사용자 정보 추출
            PrincipalDetails principalDetails = extractPrincipalDetails(authentication);
            User user = principalDetails.getUser();

            log.debug("OAuth2 사용자 인증 완료: email={}", user.getEmail());

            // 2. Spring Session에 사용자 정보 저장
            setupUserSession(request, user);

            // 3. 프론트엔드로 성공 리다이렉트
            String redirectUrl = buildSuccessRedirectUrl();
            log.info("OAuth2 로그인 성공: email={}, redirectUrl={}", user.getEmail(), redirectUrl);

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생", e);
            handleOAuthError(request, response, e);
        }
    }

    /**
     * Authentication 객체에서 PrincipalDetails 추출
     * 
     * @param authentication Spring Security 인증 객체
     * @return PrincipalDetails 객체
     * @throws AuthException 인증 정보 추출 실패 시
     */
    private PrincipalDetails extractPrincipalDetails(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof PrincipalDetails)) {
            log.error("OAuth2 인증 객체 타입 오류: expected=PrincipalDetails, actual={}",
                    principal.getClass().getSimpleName());
            throw AuthException.invalidToken();
        }

        return (PrincipalDetails) principal;
    }

    /**
     * Spring Session에 사용자 정보 설정
     * 
     * @param request HTTP 요청 객체
     * @param user    사용자 엔티티
     */
    private void setupUserSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true); // 세션이 없으면 새로 생성

        // 세션 고정 공격 방지를 위한 세션 ID 재생성
        request.changeSessionId();

        // 사용자 정보를 세션에 저장
        session.setAttribute("userId", user.getId());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userProfileImage", user.getProfileImage());

        // 세션 유효시간 설정 (30분)
        session.setMaxInactiveInterval(1800);

        log.debug("Spring Session 설정 완료: userId={}, sessionId={}",
                user.getId(), session.getId());
    }

    /**
     * 성공 리다이렉트 URL 구성
     * 
     * @return 프론트엔드 콜백 URL
     */
    private String buildSuccessRedirectUrl() {
        // v4.2에서는 단순한 성공 리다이렉트만 수행
        // 세션 정보는 이미 설정되었으므로 별도 파라미터 불필요
        return frontendCallbackUrl + "?status=success";
    }

    /**
     * OAuth 오류 처리
     * 
     * @param request  HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param error    발생한 오류
     * @throws IOException I/O 처리 중 오류 발생 시
     */
    private void handleOAuthError(HttpServletRequest request, HttpServletResponse response, Exception error)
            throws IOException {

        log.error("OAuth2 로그인 처리 중 오류 발생", error);

        // 오류 발생 시 세션 무효화
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 에러 페이지로 리다이렉트
        String errorUrl = frontendCallbackUrl + "?status=error&message=oauth_error";
        response.sendRedirect(errorUrl);
    }
}