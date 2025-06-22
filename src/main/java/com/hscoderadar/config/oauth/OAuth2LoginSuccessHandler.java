package com.hscoderadar.config.oauth;

import com.hscoderadar.common.exception.AuthException;
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
 * API 명세서 v2.4 기준 OAuth2 로그인 성공 후 JWT 토큰 생성 및 쿠키 설정을 처리하는 핸들러
 * 
 * <h3>v2.4 주요 개선사항:</h3>
 * <ul>
 * <li>강화된 OAuth 에러 처리 (OAUTH_001, OAUTH_002, OAUTH_003)</li>
 * <li>사용자 열거 공격 방지를 위한 통합 에러 처리</li>
 * <li>HttpOnly 쿠키 보안 정책 강화</li>
 * <li>프로필 이미지 처리 개선</li>
 * </ul>
 * 
 * <h3>처리 과정:</h3>
 * <ol>
 * <li>OAuth2 인증 정보에서 사용자 정보 추출</li>
 * <li>기존 사용자 조회 또는 신규 사용자 생성</li>
 * <li>JWT 토큰 생성 및 HttpOnly 쿠키 설정</li>
 * <li>프론트엔드로 리다이렉트</li>
 * </ol>
 * 
 * <h3>보안 특징:</h3>
 * <ul>
 * <li>모든 OAuth 관련 오류를 통합적으로 처리</li>
 * <li>HttpOnly, Secure, SameSite=Strict 쿠키 정책</li>
 * <li>사용자 정보 최소화 원칙 적용</li>
 * <li>프로필 이미지 URL 검증 및 안전한 처리</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 2.4.0
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
     * OAuth2 로그인 성공 시 JWT 토큰 생성 및 쿠키 설정 후 리다이렉트 (API 명세서 v2.4 기준)
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

            log.debug("OAuth2 사용자 인증 완료: email={}, provider={}",
                    user.getEmail(), user.getRegistrationType());

            // 2. 사용자 정보 업데이트 (프로필 이미지 등)
            updateUserProfileIfNeeded(user, principalDetails);

            // 3. JWT 토큰 생성
            String accessToken = generateJwtToken(authentication);

            // 4. HttpOnly 쿠키 설정
            setSecureJwtCookie(response, accessToken);

            // 5. 프론트엔드로 성공 리다이렉트
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
     * 사용자 프로필 정보 업데이트 (프로필 이미지 등)
     * 
     * @param user             사용자 엔티티
     * @param principalDetails OAuth2 사용자 상세 정보
     */
    private void updateUserProfileIfNeeded(User user, PrincipalDetails principalDetails) {
        try {
            boolean needsUpdate = false;

            // OAuth2UserInfo에서 최신 프로필 이미지 URL 확인
            String currentProfileImage = user.getProfileImage();
            String newProfileImage = extractProfileImageFromOAuth(principalDetails);

            // 프로필 이미지가 변경된 경우에만 업데이트
            if (newProfileImage != null && !newProfileImage.equals(currentProfileImage)) {
                if (isValidProfileImageUrl(newProfileImage)) {
                    user.setProfileImage(newProfileImage);
                    needsUpdate = true;
                    log.debug("프로필 이미지 업데이트: email={}", user.getEmail());
                } else {
                    log.warn("유효하지 않은 프로필 이미지 URL: email={}, url={}",
                            user.getEmail(), newProfileImage);
                }
            }

            if (needsUpdate) {
                userRepository.save(user);
                log.info("OAuth2 사용자 프로필 업데이트 완료: email={}", user.getEmail());
            }

        } catch (Exception e) {
            // 프로필 업데이트 실패는 로그인 성공에 영향을 주지 않음
            log.warn("OAuth2 사용자 프로필 업데이트 실패: email={}", user.getEmail(), e);
        }
    }

    /**
     * OAuth2 정보에서 프로필 이미지 URL 추출
     * 
     * @param principalDetails OAuth2 사용자 상세 정보
     * @return 프로필 이미지 URL (없으면 null)
     */
    private String extractProfileImageFromOAuth(PrincipalDetails principalDetails) {
        try {
            // OAuth2UserInfo가 있는 경우 프로필 이미지 추출
            // 실제 구현에서는 CustomOAuth2UserService에서 설정한 정보를 사용
            return principalDetails.getAttribute("picture"); // Google
        } catch (Exception e) {
            log.debug("프로필 이미지 추출 실패", e);
            return null;
        }
    }

    /**
     * 프로필 이미지 URL 유효성 검증
     * 
     * @param imageUrl 검증할 이미지 URL
     * @return 유효한 URL인지 여부
     */
    private boolean isValidProfileImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }

        // HTTPS URL만 허용 (보안 정책)
        if (!imageUrl.startsWith("https://")) {
            return false;
        }

        // 허용된 도메인 검증 (실제 구현에서는 설정파일로 관리)
        String[] allowedDomains = {
                "lh3.googleusercontent.com", // Google
                "ssl.pstatic.net", // Naver
                "k.kakaocdn.net" // Kakao
        };

        for (String domain : allowedDomains) {
            if (imageUrl.contains(domain)) {
                return true;
            }
        }

        log.warn("허용되지 않은 프로필 이미지 도메인: {}", imageUrl);
        return false;
    }

    /**
     * JWT 토큰 생성
     * 
     * @param authentication Spring Security 인증 객체
     * @return JWT Access Token
     * @throws AuthException 토큰 생성 실패 시
     */
    private String generateJwtToken(Authentication authentication) {
        try {
            return jwtTokenProvider.generateToken(authentication).accessToken();
        } catch (Exception e) {
            log.error("OAuth2 JWT 토큰 생성 실패", e);
            throw AuthException.invalidToken();
        }
    }

    /**
     * 보안이 강화된 HttpOnly JWT 쿠키 설정 (API 명세서 v2.4 기준)
     * 
     * @param response    HTTP 응답 객체
     * @param accessToken JWT Access Token
     */
    private void setSecureJwtCookie(HttpServletResponse response, String accessToken) {
        Cookie jwtCookie = new Cookie("token", accessToken);

        // v2.4 보안 정책 적용
        jwtCookie.setHttpOnly(true); // JavaScript 접근 차단 (XSS 방지)
        jwtCookie.setSecure(true); // HTTPS에서만 전송
        jwtCookie.setPath("/"); // 전체 경로에서 사용 가능
        jwtCookie.setMaxAge(60 * 60); // 1시간 (JWT 만료 시간과 동일)
        jwtCookie.setAttribute("SameSite", "Strict"); // CSRF 방지

        response.addCookie(jwtCookie);
        log.debug("OAuth2 JWT 쿠키 설정 완료");
    }

    /**
     * 성공 리다이렉트 URL 생성
     * 
     * @return 프론트엔드 성공 페이지 URL
     */
    private String buildSuccessRedirectUrl() {
        // 실제 구현에서는 상태 정보나 토큰 정보를 쿼리 파라미터로 전달할 수 있음
        // 하지만 v2.4에서는 HttpOnly 쿠키를 사용하므로 단순 리다이렉트
        return frontendCallbackUrl + "?status=success";
    }

    /**
     * OAuth 에러 처리 및 에러 페이지로 리다이렉트 (API 명세서 v2.4 기준)
     * 
     * @param request  HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param error    발생한 예외
     */
    private void handleOAuthError(HttpServletRequest request, HttpServletResponse response, Exception error) {
        try {
            String errorCode;
            String errorMessage;

            // v2.4 에러 코드 매핑
            if (error instanceof AuthException) {
                AuthException authError = (AuthException) error;
                errorCode = authError.getErrorCode().name();
                errorMessage = authError.getMessage();
            } else {
                // 예상치 못한 오류는 OAUTH_002로 통일
                errorCode = "OAUTH_002";
                errorMessage = "소셜 로그인에 실패했습니다";
            }

            log.error("OAuth2 로그인 에러 처리: code={}, message={}", errorCode, errorMessage);

            // 에러 정보와 함께 프론트엔드 에러 페이지로 리다이렉트
            String errorUrl = String.format("%s?error=%s&message=%s",
                    frontendCallbackUrl, errorCode, errorMessage);

            response.sendRedirect(errorUrl);

        } catch (IOException e) {
            log.error("OAuth2 에러 리다이렉트 실패", e);
            // 최후의 수단으로 HTTP 500 응답
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}