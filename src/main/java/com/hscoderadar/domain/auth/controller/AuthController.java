package com.hscoderadar.domain.auth.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.common.exception.RateLimitException;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.auth.service.AuthService;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import com.hscoderadar.domain.auth.service.AuthService.TokenRefreshResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

/**
 * v6.1 변경된 JWT 토큰 정책을 적용한 인증 시스템
 * 
 * v6.1 JWT 토큰 정책:
 * - Access Token (30분): Authorization Bearer 헤더로 전송, JSON 응답으로 반환
 * - Refresh Token (1일/30일): HttpOnly 쿠키로 관리, /api/auth/refresh에서만 사용
 * - 보안성과 편의성을 균형있게 제공
 * 
 * 보안 강화 기능:
 * - HttpOnly 쿠키 경로를 /api/auth/refresh로 제한
 * - Token Rotation 보안 정책 (재사용 방지)
 * - 사용자 열거 공격 방지 (모든 인증 실패 통일 처리)
 * - IP 기반 Rate Limiting (5회/15분)
 * 
 * 응답 형식:
 * - Access Token은 JSON으로 반환 (프론트엔드 상태관리용)
 * - Refresh Token은 HttpOnly 쿠키로 설정 (XSS 방지)
 * - expiresIn 필드로 토큰 만료 시간 제공
 * 
 * @author HsCodeRadar Team
 * @since 6.1.0
 * @see AuthService
 * @see ApiResponseMessage
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 새로운 사용자 계정 생성 (v6.1 명세 기준)
     * 
     * 📊 HTTP 상태 코드 매트릭스:
     * - ✅ 201 Created: 성공
     * - ❌ 409 Conflict: 이메일 중복 (USER_001)
     * - ❌ 400 Bad Request: 입력 데이터 오류 (USER_002)
     * - ❌ 422 Unprocessable Entity: 비밀번호 정책 위반 (USER_004)
     * - ❌ 500 Internal Server Error: 서버 오류 (COMMON_002)
     * 
     * @param request 회원가입 요청 정보 (이메일, 비밀번호, 이름)
     * @return v6.1 보안 정책에 따른 최소 사용자 정보
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponseMessage("계정이 생성되었습니다")
    public Map<String, Object> register(@RequestBody SignUpRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());

        try {
            User savedUser = authService.signUp(request);

            // v6.1 보안 정책: 최소 정보만 제공
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", savedUser.getEmail());
            userData.put("name", savedUser.getName());
            userData.put("profileImage", savedUser.getProfileImage());

            log.info("회원가입 완료: email={}", savedUser.getEmail());
            return userData;

        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    /**
     * 사용자 로그인 처리 및 v6.1 변경된 JWT 토큰 발급
     * 
     * v6.1 변경된 JWT 토큰 정책:
     * - Access Token (30분): JSON 응답으로 반환 (Authorization Bearer 헤더용)
     * - Refresh Token (1일/30일): HttpOnly 쿠키로 설정 (XSS 방지)
     * 
     * HTTP 상태 코드:
     * - ✅ 200 OK: 성공
     * - ❌ 401 Unauthorized: 인증 실패 (AUTH_001)
     * - ❌ 423 Locked: 계정 잠김 (AUTH_002)
     * - ❌ 400 Bad Request: 입력 데이터 누락 (COMMON_001)
     * - ❌ 429 Too Many Requests: 로그인 시도 한도 초과 (RATE_LIMIT_001)
     * 
     * @param request     로그인 요청 정보 (이메일, 비밀번호, rememberMe)
     * @param response    HTTP 응답 객체 (Refresh Token 쿠키 설정용)
     * @param httpRequest HTTP 요청 객체 (Rate Limiting용)
     * @return Access Token + 사용자 정보 (JSON 형태)
     */
    @PostMapping("/login")
    @ApiResponseMessage("인증되었습니다")
    public Map<String, Object> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response,
            HttpServletRequest httpRequest) {

        log.info("로그인 요청: email={}, rememberMe={}", request.getEmail(), request.isRememberMe());

        try {
            // Rate Limiting 체크 (IP 기반)
            authService.checkLoginRateLimit(httpRequest.getRemoteAddr());

            // v6.1 JWT 세부화: remember me 옵션을 고려한 토큰 생성
            TokenInfo tokenInfo = authService.loginWithToken(request);
            User user = authService.findUserByEmail(request.getEmail());

            // v6.1 변경: Refresh Token을 HttpOnly 쿠키에 설정
            Cookie refreshTokenCookie = new Cookie("refreshToken", tokenInfo.refreshToken());
            refreshTokenCookie.setHttpOnly(true); // JavaScript 접근 불가 (XSS 방지)
            refreshTokenCookie.setSecure(true); // HTTPS에서만 전송
            refreshTokenCookie.setPath("/api/auth/refresh"); // refresh 엔드포인트에서만 사용
            refreshTokenCookie.setAttribute("SameSite", "Strict"); // CSRF 방지

            // remember me 설정에 따라 쿠키 수명 결정
            if (request.isRememberMe()) {
                refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
                log.debug("Refresh Token 쿠키 설정: 30일 (remember me)");
            } else {
                refreshTokenCookie.setMaxAge(24 * 60 * 60); // 1일
                log.debug("Refresh Token 쿠키 설정: 1일 (일반)");
            }

            response.addCookie(refreshTokenCookie);

            // v6.1 변경: Access Token은 JSON으로 반환, Refresh Token은 HttpOnly 쿠키
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("profileImage", user.getProfileImage());
            userData.put("phoneVerified", user.getPhoneVerified() != null ? user.getPhoneVerified() : false);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", tokenInfo.accessToken()); // JSON으로 반환 (프론트엔드 상태관리용)
            responseData.put("tokenType", "Bearer");
            responseData.put("expiresIn", 1800); // Access Token 만료 시간 (30분 = 1800초)
            responseData.put("user", userData);

            log.info("로그인 성공: email={}, rememberMe={}", user.getEmail(), request.isRememberMe());
            return responseData;

        } catch (RateLimitException e) {
            log.warn("로그인 시도 한도 초과: ip={}, email={}", httpRequest.getRemoteAddr(), request.getEmail());
            throw e;
        } catch (Exception e) {
            log.warn("로그인 실패: email={}, reason={}", request.getEmail(), e.getMessage());
            // v6.1: 사용자 열거 공격 방지 - 모든 인증 실패를 AUTH_001로 통일
            throw AuthException.invalidCredentials();
        }
    }

    /**
     * 현재 JWT 토큰 상태 확인 및 사용자 정보 반환 (v6.1 명세 기준)
     * 
     * 📊 HTTP 상태 코드 매트릭스:
     * - ✅ 200 OK: 유효한 토큰
     * - ❌ 401 Unauthorized: 토큰 만료 (AUTH_003)
     * - ❌ 401 Unauthorized: 유효하지 않은 토큰 (AUTH_004)
     * - ❌ 401 Unauthorized: 토큰 없음 (AUTH_004)
     * 
     * @param principalDetails 인증된 사용자 정보 (Spring Security Context에서 주입)
     * @return v6.1 사용자 정보 + rememberMe 상태
     */
    @GetMapping("/verify")
    @ApiResponseMessage("인증 상태 확인됨")
    public Map<String, Object> verify(@AuthenticationPrincipal PrincipalDetails principalDetails) {

        if (principalDetails == null) {
            log.debug("인증 정보 없음");
            throw AuthException.invalidToken();
        }

        User user = principalDetails.getUser();
        log.debug("인증 상태 확인: email={}", user.getEmail());

        // v6.1 응답 형식: 사용자 정보 + remember me 상태
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("profileImage", user.getProfileImage());
        userData.put("phoneVerified", user.getPhoneVerified() != null ? user.getPhoneVerified() : false);
        userData.put("rememberMe", user.getRememberMeEnabled() != null ? user.getRememberMeEnabled() : false);

        return userData;
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token 발급
     * 
     * v6.1 Token Rotation 보안 정책:
     * - 기존 Refresh Token 무효화
     * - 새로운 토큰 쌍 발급
     * - 재사용 방지
     * - 데이터베이스 검증
     * 
     * HTTP 상태 코드:
     * - ✅ 200 OK: 갱신 성공
     * - ❌ 400 Bad Request: Refresh Token 없음 (AUTH_001)
     * - ❌ 401 Unauthorized: 유효하지 않은 토큰 (AUTH_003)
     * - ❌ 401 Unauthorized: 만료된 토큰 (AUTH_003)
     * - ❌ 401 Unauthorized: DB 토큰 불일치 (AUTH_004)
     * 
     * v6.1 변경사항: HttpOnly 쿠키에서 Refresh Token을 추출하여 보안 강화
     * 
     * @param httpRequest HTTP 요청 객체 (HttpOnly 쿠키에서 Refresh Token 추출용)
     * @param response    HTTP 응답 객체 (새 Refresh Token 쿠키 설정용)
     * @return 새로 발급된 Access Token 정보 (JSON 형태)
     */
    @PostMapping("/refresh")
    @ApiResponseMessage("토큰이 갱신되었습니다")
    public Map<String, Object> refresh(HttpServletRequest httpRequest, HttpServletResponse response) {
        log.info("토큰 갱신 요청");

        // v6.1: HttpOnly 쿠키에서 Refresh Token 추출
        String refreshToken = null;
        if (httpRequest.getCookies() != null) {
            for (Cookie cookie : httpRequest.getCookies()) {
                if ("refreshToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    refreshToken = cookie.getValue().trim();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            log.warn("Refresh Token 쿠키 누락");
            throw AuthException.invalidToken();
        }

        try {
            log.debug("토큰 갱신 시작 - refreshToken 길이: {}", refreshToken.length());

            // 🆕 v6.1 Token Rotation: 새로운 토큰 쌍 발급 + 기존 토큰 무효화
            TokenRefreshResult result = authService.refreshTokens(refreshToken);
            TokenInfo newTokenInfo = result.tokenInfo();
            boolean rememberMe = result.rememberMe();

            // v6.1: 새로운 Refresh Token을 HttpOnly 쿠키로 업데이트
            Cookie newRefreshTokenCookie = new Cookie("refreshToken", newTokenInfo.refreshToken());
            newRefreshTokenCookie.setHttpOnly(true);
            newRefreshTokenCookie.setSecure(true);
            newRefreshTokenCookie.setPath("/api/auth/refresh"); // refresh 엔드포인트에서만 사용
            newRefreshTokenCookie.setAttribute("SameSite", "Strict");

            // Remember Me 설정에 따라 쿠키 수명 설정
            if (rememberMe) {
                newRefreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
            } else {
                newRefreshTokenCookie.setMaxAge(24 * 60 * 60); // 1일
            }

            response.addCookie(newRefreshTokenCookie);

            // v6.1 변경: Access Token을 JSON으로 반환 (프론트엔드 상태관리용)
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("accessToken", newTokenInfo.accessToken());
            tokenData.put("tokenType", "Bearer");
            tokenData.put("expiresIn", 1800); // Access Token 만료 시간 (30분)
            tokenData.put("rememberMe", rememberMe); // Remember Me 상태 포함

            log.info("토큰 갱신 완료: rememberMe={}", rememberMe);
            return tokenData;

        } catch (AuthException e) {
            log.error("토큰 갱신 실패 - AuthException: code={}, message={}",
                    e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN",
                    e.getMessage());
            throw e; // AuthException은 그대로 다시 던짐
        } catch (Exception e) {
            log.error("토큰 갱신 중 예상치 못한 오류", e);
            throw AuthException.tokenExpired();
        }
    }

    /**
     * 사용자 로그아웃 처리 및 v6.1 변경된 토큰 정리 정책
     * 
     * v6.1 변경된 토큰 정리 정책:
     * - Access Token: 클라이언트에서 삭제 (JSON으로 받았으므로 프론트엔드에서 처리)
     * - Refresh Token: HttpOnly 쿠키 삭제
     * - 데이터베이스에서 Refresh Token 무효화
     * 
     * HTTP 상태 코드:
     * - ✅ 204 No Content: 성공
     * - ✅ 200 OK: 이미 로그아웃 상태
     * 
     * @param principalDetails 인증된 사용자 정보
     * @param response         HTTP 응답 객체 (쿠키 삭제용)
     * @return 204 No Content (응답 본문 없음)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            HttpServletResponse response) {

        if (principalDetails != null) {
            String userEmail = principalDetails.getUser().getEmail();
            log.info("로그아웃 요청: email={}", userEmail);

            // v6.1: PostgreSQL에서 Refresh Token 무효화
            authService.logout(userEmail);

            log.info("로그아웃 완료: email={}", userEmail);
        } else {
            log.debug("이미 로그아웃 상태");
        }

        // v6.1 변경: Refresh Token HttpOnly 쿠키 삭제
        // 쿠키 값을 null 대신 빈 문자열로 설정하여 안정적으로 삭제
        Cookie refreshTokenCookie = new Cookie("refreshToken", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/api/auth/refresh"); // 로그인 시 설정된 경로와 정확히 일치해야 함
        refreshTokenCookie.setMaxAge(0); // 즉시 만료
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);

        log.debug("Refresh Token 쿠키 삭제 시도: path={}", refreshTokenCookie.getPath());

        // 호환성을 위해 기존 쿠키들도 정리
        // 잘못 설정되었던 Path=/ 쿠키 정리
        Cookie misconfiguredCookie = new Cookie("refreshToken", "");
        misconfiguredCookie.setHttpOnly(true);
        misconfiguredCookie.setSecure(true);
        misconfiguredCookie.setPath("/");
        misconfiguredCookie.setMaxAge(0);
        misconfiguredCookie.setAttribute("SameSite", "Strict");
        response.addCookie(misconfiguredCookie);

        // 레거시 refresh_token 쿠키 정리
        Cookie legacyRefreshCookie = new Cookie("refresh_token", "");
        legacyRefreshCookie.setHttpOnly(true);
        legacyRefreshCookie.setSecure(true);
        legacyRefreshCookie.setPath("/");
        legacyRefreshCookie.setMaxAge(0);
        legacyRefreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(legacyRefreshCookie);

        // v6.1 표준: 로그아웃은 204 No Content
        return ResponseEntity.noContent().build();
    }

    /**
     * OAuth2 소셜 로그인 시작 (v6.1 명세서 준수)
     * 
     * 명세서 요구사항:
     * - GET /api/oauth2/authorization/{provider}
     * - Query Parameters: rememberMe (boolean)
     * - Response: 302 Found with Location header
     * - 에러 코드: OAUTH_001, OAUTH_002, OAUTH_003
     */
    @GetMapping("/oauth2/authorization/{provider}")
    public RedirectView startOAuth2Login(
            @PathVariable String provider,
            @RequestParam(defaultValue = "false") boolean rememberMe,
            HttpServletRequest request) {

        log.info("OAuth2 로그인 시작: provider={}, rememberMe={}", provider, rememberMe);

        // 지원하는 OAuth 제공업체 검증
        List<String> supportedProviders = Arrays.asList("google", "naver", "kakao");
        if (!supportedProviders.contains(provider.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다");
        }

        // rememberMe 상태를 세션에 임시 저장 (OAuth 완료 후 사용)
        request.getSession().setAttribute("rememberMe", rememberMe);

        // Spring Security OAuth2 엔드포인트로 리디렉션
        String redirectUrl = "/oauth2/authorization/" + provider.toLowerCase();

        log.debug("OAuth2 리디렉션: {}", redirectUrl);
        return new RedirectView(redirectUrl, true);
    }
}