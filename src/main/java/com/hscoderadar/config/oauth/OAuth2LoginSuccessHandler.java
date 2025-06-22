package com.hscoderadar.config.oauth;

import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import com.hscoderadar.domain.users.entity.User;
import com.hscoderadar.domain.users.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * JWT 기반 인증 시스템을 위한 OAuth2 로그인 성공 핸들러
 * 
 * <p>
 * 이 핸들러는 Google, Naver, Kakao 등의 SNS 로그인이 성공한 후
 * HttpOnly 쿠키 기반 JWT 토큰 관리를 통한 안전한 인증 처리를 수행합니다:
 * <ul>
 * <li>JWT Access Token을 HttpOnly 쿠키에 설정</li>
 * <li>Refresh Token을 데이터베이스에 저장</li>
 * <li>프론트엔드 콜백 URL로 성공/실패 상태만 전달</li>
 * </ul>
 * 
 * <p>
 * <strong>JWT 기반 인증 아키텍처:</strong>
 * <ul>
 * <li>서버: JWT 토큰을 HttpOnly 쿠키로 자동 관리</li>
 * <li>클라이언트: 사용자 정보만 상태 관리, 토큰은 서버 의존</li>
 * <li>보안: XSS 완전 차단, CSRF 방지, 자동 토큰 동기화</li>
 * </ul>
 * 
 * <p>
 * <strong>프론트엔드 연동 플로우:</strong>
 * <ol>
 * <li>OAuth 로그인 성공 시 JWT를 HttpOnly 쿠키에 설정</li>
 * <li>프론트엔드 콜백으로 성공 상태만 전달</li>
 * <li>프론트엔드에서 /api/auth/verify 호출하여 사용자 정보 조회</li>
 * <li>사용자 정보를 클라이언트 상태에 저장하여 UI 업데이트</li>
 * </ol>
 * 
 * @author HsCodeRadar Team
 * @since 2.1.0
 * @see JwtTokenProvider
 * @see CustomOAuth2UserService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * 프론트엔드 콜백 URL을 application.properties에서 주입받음
     * 
     * <p>
     * 기본값: {@code http://localhost:3000/auth/callback}
     * <p>
     * 배포 환경에서는 실제 프론트엔드 도메인으로 설정 필요
     * 
     * <h3>설정 예시:</h3>
     * 
     * <pre>
     * # application.properties
     * oauth2.frontend.callback-url=https://your-frontend-domain.com/auth/callback
     * </pre>
     */
    @Value("${oauth2.frontend.callback-url:http://localhost:3000/auth/callback}")
    private String frontendCallbackUrl;

    /**
     * OAuth2 로그인 성공 시 HttpOnly 쿠키 기반 인증 처리를 수행합니다.
     * 
     * <p>
     * 이 메서드는 Spring Security OAuth2 클라이언트에 의해 자동으로 호출되며,
     * SNS 인증이 완료된 후 JWT 기반 인증 시스템에 맞는 처리를 수행합니다.
     * 
     * <h3>처리 순서:</h3>
     * <ol>
     * <li>인증된 사용자 정보 확인</li>
     * <li>JWT Access Token 및 Refresh Token 생성</li>
     * <li>Access Token을 HttpOnly 쿠키에 설정</li>
     * <li>Refresh Token을 데이터베이스에 저장</li>
     * <li>프론트엔드 콜백 URL로 성공 상태만 전달</li>
     * </ol>
     * 
     * <h3>쿠키 보안 설정:</h3>
     * <ul>
     * <li>HttpOnly: JavaScript 접근 불가 (XSS 방지)</li>
     * <li>Secure: HTTPS에서만 전송</li>
     * <li>SameSite=Strict: CSRF 공격 방지</li>
     * <li>Path=/: 전체 도메인에서 사용 가능</li>
     * <li>Max-Age=604800: 7일간 유지 (Remember Me 효과)</li>
     * </ul>
     * 
     * <h3>프론트엔드 처리 요구사항:</h3>
     * <ul>
     * <li>{@code /auth/callback?success=true} 라우트 구현</li>
     * <li>성공 시 {@code /api/auth/verify} 호출하여 사용자 정보 조회</li>
     * <li>사용자 정보를 클라이언트 상태에 저장</li>
     * <li>대시보드 또는 메인 페이지로 리디렉션</li>
     * </ul>
     * 
     * <h3>오류 처리:</h3>
     * <ul>
     * <li>사용자 조회 실패 시 error=user_not_found</li>
     * <li>토큰 생성 실패 시 error=oauth2_processing_failed</li>
     * <li>예외 발생 시 error=oauth2_processing_failed</li>
     * </ul>
     * 
     * @param request        HTTP 요청 객체
     * @param response       HTTP 응답 객체 (쿠키 설정 및 리디렉션에 사용)
     * @param authentication OAuth2 인증 결과 객체 (사용자 정보 포함)
     * @throws IOException              리디렉션 처리 중 I/O 오류가 발생한 경우
     * @throws ServletException         서블릿 처리 중 오류가 발생한 경우
     * @throws IllegalArgumentException 인증된 사용자를 데이터베이스에서 찾을 수 없는 경우
     * 
     * @see Authentication
     * @see TokenInfo
     * @see HttpServletResponse#sendRedirect(String)
     */
    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String userEmail = authentication.getName();
        log.info("OAuth2 로그인 성공: email={}", userEmail);

        try {
            // JWT 토큰 발급
            TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);
            log.debug("JWT 토큰 생성 완료: email={}", userEmail);

            // DB에 Refresh Token 저장
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.error("OAuth2 인증 후 사용자 조회 실패: email={}", userEmail);
                        return new IllegalArgumentException("OAuth2 인증 후 사용자를 찾을 수 없습니다: " + userEmail);
                    });

            user.setRefreshToken(tokenInfo.refreshToken());
            log.debug("Refresh Token 저장 완료: userId={}", user.getId());

            // JWT를 HttpOnly 쿠키에 설정
            Cookie jwtCookie = new Cookie("token", tokenInfo.accessToken());
            jwtCookie.setHttpOnly(true); // JavaScript 접근 불가
            jwtCookie.setSecure(true); // HTTPS에서만 전송
            jwtCookie.setPath("/"); // 전체 경로에서 사용
            jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7일 (OAuth는 Remember Me 기본값)
            jwtCookie.setAttribute("SameSite", "Strict"); // CSRF 방지

            response.addCookie(jwtCookie);
            log.debug("JWT 쿠키 설정 완료: userId={}", user.getId());

            // 프론트엔드 콜백 URL로 성공 상태만 전달
            String successUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                    .queryParam("success", "true")
                    .build()
                    .toUriString();

            log.info("OAuth2 로그인 완료, 프론트엔드로 리디렉션: userId={}, redirectUrl={}",
                    user.getId(), frontendCallbackUrl);

            response.sendRedirect(successUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 후 처리 중 오류 발생: email={}", userEmail, e);

            // 오류 발생 시 프론트엔드 오류 페이지로 리디렉션
            String errorUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                    .queryParam("error", "oauth2_processing_failed")
                    .queryParam("message", "로그인 처리 중 오류가 발생했습니다")
                    .build()
                    .toUriString();

            response.sendRedirect(errorUrl);
        }
    }
}