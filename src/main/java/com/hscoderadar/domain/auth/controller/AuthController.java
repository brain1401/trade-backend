package com.hscoderadar.domain.auth.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.common.exception.RateLimitException;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.RefreshTokenRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.auth.service.AuthService;
import com.hscoderadar.domain.users.entity.User;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * API 명세서 v4.0 기준 JWT 기반 인증 시스템을 위한 REST API 컨트롤러
 * 
 * 이 컨트롤러는 Public API와 Private API를 구분하여 차별화된 보안 정책을 적용하는
 * JWT 기반 인증 시스템을 구현
 * 
 * <h3>v4.0 주요 개선사항:</h3>
 * <ul>
 * <li>완전한 HTTP 상태 코드 매트릭스 적용</li>
 * <li>39개 포괄적 에러 코드 체계 구현</li>
 * <li>사용자 열거 공격 방지 정책 강화</li>
 * <li>DELETE 작업 표준화 (204 No Content)</li>
 * <li>토큰 갱신 API 추가 (Refresh Token 지원)</li>
 * <li>ResponseWrapperAdvice 완전 호환 (직접 객체 반환)</li>
 * </ul>
 * 
 * <h3>보안 특징:</h3>
 * <ul>
 * <li>HttpOnly 쿠키 기반 JWT 토큰 관리 (XSS 완전 차단)</li>
 * <li>CSRF 방지를 위한 SameSite=Strict 설정</li>
 * <li>모든 인증 실패를 AUTH_001로 통일 처리</li>
 * <li>Rate Limiting 적용 (로그인 시도 제한)</li>
 * <li>Token Rotation 보안 정책 적용</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 4.0.0
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
     * 새로운 사용자 계정 생성 (API 명세서 v4.0 기준)
     * 
     * HTTP 상태 코드 매트릭스:
     * - 201 Created: 성공
     * - 409 Conflict: 이메일 중복
     * - 400 Bad Request: 입력 데이터 오류
     * - 422 Unprocessable Entity: 비밀번호 정책 위반
     * - 500 Internal Server Error: 서버 오류
     * 
     * ResponseWrapperAdvice가 자동으로 ApiResponse 형태로 래핑하여 응답합니다.
     * 
     * @param request 회원가입 요청 정보 (이메일, 비밀번호, 이름 포함)
     * @return v4.0 보안 정책에 따른 최소 사용자 정보 (자동으로 ApiResponse로 래핑됨)
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // 201 Created 상태 코드 설정
    @ApiResponseMessage("계정이 생성되었습니다")
    public Map<String, Object> register(@RequestBody SignUpRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());

        try {
            User savedUser = authService.signUp(request);

            // v4.0 보안 정책: 클라이언트에 최소 정보만 제공
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", savedUser.getEmail());
            userData.put("name", savedUser.getName());
            userData.put("profileImage", savedUser.getProfileImage());

            log.info("회원가입 완료: email={}", savedUser.getEmail());
            return userData;

        } catch (IllegalArgumentException e) {
            // 에러는 GlobalExceptionHandler에서 처리
            throw e;
        }
    }

    /**
     * 사용자 로그인 처리 및 JWT 토큰을 HttpOnly 쿠키에 설정 (API 명세서 v4.0 기준)
     * 
     * HTTP 상태 코드 매트릭스:
     * - 200 OK: 성공
     * - 401 Unauthorized: 인증 실패 (등록되지 않은 사용자, 비밀번호 불일치)
     * - 423 Locked: 계정 잠김
     * - 400 Bad Request: 입력 데이터 누락
     * - 429 Too Many Requests: 로그인 시도 한도 초과
     * 
     * 🛡️ 보안 정책: 사용자 열거 공격 방지를 위해 모든 인증 실패를 동일하게 처리
     * 
     * ResponseWrapperAdvice가 자동으로 ApiResponse 형태로 래핑하여 응답합니다.
     * 
     * @param request     로그인 요청 정보 (이메일, 비밀번호, Remember Me)
     * @param response    HTTP 응답 객체 (쿠키 설정용)
     * @param httpRequest HTTP 요청 객체 (Rate Limiting용)
     * @return v4.0 보안 정책에 따른 최소 사용자 정보 (자동으로 ApiResponse로 래핑됨)
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

            String jwtToken = authService.loginWithCookie(request);
            User user = authService.findUserByEmail(request.getEmail());

            // JWT를 HttpOnly 쿠키에 설정
            Cookie jwtCookie = new Cookie("token", jwtToken);
            jwtCookie.setHttpOnly(true); // JavaScript 접근 불가
            jwtCookie.setSecure(true); // HTTPS에서만 전송
            jwtCookie.setPath("/"); // 전체 경로에서 사용
            jwtCookie.setAttribute("SameSite", "Strict"); // CSRF 방지

            // Remember Me 설정에 따라 쿠키 수명 결정
            if (request.isRememberMe()) {
                jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
            }
            // else: 세션 쿠키 (브라우저 종료 시 삭제)

            response.addCookie(jwtCookie);

            // v4.0 보안 정책: 클라이언트에 최소 정보만 제공
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("profileImage", user.getProfileImage());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", userData);

            log.info("로그인 성공: email={}", user.getEmail());
            return responseData;

        } catch (RateLimitException e) {
            log.warn("로그인 시도 한도 초과: ip={}, email={}", httpRequest.getRemoteAddr(), request.getEmail());
            throw e;
        } catch (Exception e) {
            log.warn("로그인 실패: email={}, reason={}", request.getEmail(), e.getMessage());
            // Rate Limiting용 실패 기록은 AuthService에서 처리하도록 수정할 예정
            // 모든 인증 실패를 AUTH_001로 통일 (사용자 열거 공격 방지)
            throw AuthException.invalidCredentials();
        }
    }

    /**
     * HttpOnly 쿠키의 JWT 토큰 검증 및 사용자 정보 반환 (API 명세서 v4.0 기준)
     * 
     * HTTP 상태 코드 매트릭스:
     * - 200 OK: 유효한 토큰
     * - 401 Unauthorized: 토큰 만료, 유효하지 않은 토큰, 토큰 없음
     * 
     * ResponseWrapperAdvice가 자동으로 ApiResponse 형태로 래핑하여 응답합니다.
     * 
     * @param principalDetails 인증된 사용자 정보 (Spring Security Context에서 주입)
     * @return v4.0 보안 정책에 따른 최소 사용자 정보 (phoneVerified 포함, 자동으로 ApiResponse로 래핑됨)
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

        // v4.0 보안 정책: 클라이언트에 최소 정보만 제공 (phoneVerified 포함)
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("profileImage", user.getProfileImage());
        // API 명세서에 따라 phoneVerified 필드 추가
        userData.put("phoneVerified", user.getPhoneVerified() != null ? user.getPhoneVerified() : false);

        return userData;
    }

    /**
     * Refresh Token을 사용하여 새로운 토큰 쌍 발급 (API 명세서 v4.0 기준)
     * 
     * HTTP 상태 코드 매트릭스:
     * - 200 OK: 갱신 성공
     * - 400 Bad Request: Refresh Token 누락
     * - 401 Unauthorized: 유효하지 않은 토큰, 만료된 토큰, DB 토큰 불일치
     * 
     * Token Rotation 보안 정책:
     * - 기존 Refresh Token 무효화
     * - 새로운 토큰 쌍 발급
     * - 재사용 방지
     * 
     * ResponseWrapperAdvice가 자동으로 ApiResponse 형태로 래핑하여 응답합니다.
     * 
     * @param request Refresh Token 요청 정보
     * @return 새로 발급된 토큰 정보 (Access Token + Refresh Token, 자동으로 ApiResponse로 래핑됨)
     */
    @PostMapping("/refresh")
    @ApiResponseMessage("토큰이 갱신되었습니다")
    public Map<String, Object> refresh(@RequestBody RefreshTokenRequest request) {
        log.info("토큰 갱신 요청");

        if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            log.warn("Refresh Token 누락");
            throw AuthException.invalidToken();
        }

        try {
            TokenInfo newTokenInfo = authService.refreshTokens(request.getRefreshToken());

            // API 명세서에 따른 응답 구조
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("tokenType", "Bearer");
            tokenData.put("accessToken", newTokenInfo.accessToken());
            tokenData.put("refreshToken", newTokenInfo.refreshToken());

            log.info("토큰 갱신 완료");
            return tokenData;

        } catch (Exception e) {
            log.error("토큰 갱신 실패", e);
            throw AuthException.tokenExpired();
        }
    }

    /**
     * 사용자 로그아웃 처리 및 HttpOnly 쿠키 삭제 (API 명세서 v4.0 기준)
     * 
     * HTTP 상태 코드 매트릭스:
     * - 204 No Content: 성공 (v4.0 Breaking Change)
     * - 200 OK: 이미 로그아웃 상태
     * 
     * v4.0 Breaking Change: 200 OK → 204 No Content (응답 본문 없음)
     * 
     * 주의: 204 상태 코드를 위해 ResponseEntity 사용 (ResponseWrapperAdvice 적용 안됨)
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

            // 데이터베이스에서 Refresh Token 제거
            authService.logout(userEmail);

            log.info("로그아웃 완료: email={}", userEmail);
        } else {
            log.debug("이미 로그아웃 상태");
        }

        // HttpOnly 쿠키 삭제
        Cookie jwtCookie = new Cookie("token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // 즉시 만료
        jwtCookie.setAttribute("SameSite", "Strict");

        response.addCookie(jwtCookie);

        // v4.0 표준: DELETE 작업은 204 No Content, 응답 본문 없음
        return ResponseEntity.noContent().build();
    }
}